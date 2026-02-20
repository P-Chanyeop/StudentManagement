#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
import json
import time
import requests
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options

# Windows 인코딩 문제 해결
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')


class RenaissanceQuizScraper:
    def __init__(self, base_url="https://global-zone60.renaissance-go.com"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        })
        self.bearer_token = None
        self.assignee_id = None
        self.client_id = None

    def login(self, username, password, tenant_id="rpna78nj"):
        options = Options()
        options.add_argument('--headless')
        options.add_argument('--no-sandbox')
        options.add_argument('--disable-dev-shm-usage')

        driver = webdriver.Chrome(options=options)

        try:
            driver.get(f"{self.base_url}/studentportal/entry?t={tenant_id}")
            
            username_field = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.NAME, "Username"))
            )
            username_field.send_keys(username)
            driver.find_element(By.NAME, "Password").send_keys(password)
            driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()

            WebDriverWait(driver, 20).until(
                lambda d: '/studentportal' in d.current_url and '/identityservice' not in d.current_url
            )

            self.assignee_id = driver.execute_script("return window.appModel?.userContext?.RPUserId")
            self.client_id = driver.execute_script("return window.appModel?.userContext?.RPClientId")
            self.bearer_token = driver.execute_script("return window.appModel?.config?.AuthToken")

            driver.get(f"{self.base_url}/studentprogress/")
            time.sleep(2)

            for cookie in driver.get_cookies():
                self.session.cookies.set(cookie['name'], cookie['value'], domain=cookie.get('domain'))

            driver.quit()
            return True

        except Exception as e:
            driver.quit()
            return False

    def _api_headers(self):
        return {
            'Authorization': f'Bearer {self.bearer_token}',
            'Content-Type': 'application/json',
            'Origin': self.base_url,
            'Referer': f'{self.base_url}/studentportal/',
            'rli-clientid': self.client_id,
            'rli-userid': self.assignee_id
        }

    def get_quiz_list(self):
        payload = {
            "filters": [
                {"field": "assigneeId", "values": [self.assignee_id], "operator": "EQ"},
                {"field": "State", "values": ["Rejected", "Pending", "Archived", "Ignore"], "operator": "NE"}
            ]
        }

        inbox_resp = self.session.post(
            f"{self.base_url}/assignmentplatformservice/StudentInbox",
            headers=self._api_headers(),
            json=payload,
            timeout=15
        )

        if inbox_resp.status_code != 200:
            return []

        school_year_id = inbox_resp.json().get('currentSchoolYear', {}).get('id')

        items_payload = {
            "paging": {"currentPage": 1, "pageSize": 100},
            "filters": [
                {"field": "assigneeId", "values": [self.assignee_id], "operator": "EQ"},
                {"field": "State", "values": ["Rejected", "Pending", "Archived", "Ignore"], "operator": "NE"},
                {"field": "schoolYearId", "values": [school_year_id], "operator": "EQ"}
            ],
            "sort": ["+state", "-lastupdated"]
        }

        resp = self.session.post(
            f"{self.base_url}/assignmentplatformservice/StudentInboxItems",
            headers=self._api_headers(),
            json=items_payload,
            timeout=15
        )

        if resp.status_code != 200:
            return []

        quizzes = []
        for a in resp.json().get('data', []):
            for c in a.get('children', []):
                quizzes.append({'quiz_name': c['name'], 'activity_id': c['id']})
        return quizzes

    def get_quiz_result(self, quiz):
        activity_id = quiz.get('activity_id', '')
        url = f"{self.base_url}/studentprogress/api/topsreport/gettopsreport/{activity_id}"
        resp = self.session.get(url, headers=self._api_headers(), timeout=15)
        if resp.status_code == 200:
            try:
                return resp.json()
            except:
                return None
        return None

    def collect_all_results(self):
        quizzes = self.get_quiz_list()
        results = []
        
        for quiz in quizzes:
            result = self.get_quiz_result(quiz)
            if result and 'book' in result:
                b = result.get('book', {})
                q = result.get('quizResults', {})
                s = result.get('schoolYearSummary', {})
                
                questions = q.get('questions', 1)
                correct = q.get('correct', 0)
                percent = int(correct / questions * 100) if questions > 0 else 0
                
                # 프론트엔드 필드명에 맞춤
                results.append({
                    'title': quiz['quiz_name'],
                    'class': result.get('class', ''),
                    'bookTitle': b.get('title', ''),
                    'author': b.get('author', ''),
                    'atosLevel': str(b.get('atosBookLevel', '')),
                    'quizNumber': str(b.get('quizNumber', '')),
                    'quizDate': b.get('dateTaken', ''),
                    'interestLevel': b.get('interestLevel', ''),
                    'twi': b.get('twiType', ''),
                    'type': b.get('fictionNonfiction', ''),
                    'wordCount': str(b.get('wordCount', 0)),
                    'quizResult': f"{correct} of {questions} Correct",
                    'percentCorrect': f"{percent}%",
                    'pointsEarned': f"{q.get('pointsEarned', 0)} of {q.get('pointsAvailable', 0)}",
                    'yearSummary': {
                        'period': f"{s.get('dateProgress', {}).get('startDate', '')[:10]} - {s.get('dateProgress', {}).get('endDate', '')[:10]}",
                        'Average % Correct': f"{s.get('averagePercentCorrect', 0)}%",
                        'Average ATOS Book Level': str(s.get('averageAtosBookLevel', 0)),
                        'Points Earned': str(s.get('pointsEarned', 0)),
                        'Quizzes Passed': f"{s.get('quizzesPassed', 0)} of {s.get('quizzesTaken', 0)}",
                        'Words Read': f"{s.get('wordsRead', 0):,}",
                    }
                })
            time.sleep(0.2)
        return results


def scrape_quizzes(username, password):
    scraper = RenaissanceQuizScraper()
    if scraper.login(username, password):
        return scraper.collect_all_results()
    return []


if __name__ == '__main__':
    if len(sys.argv) < 3:
        sys.exit(1)
    
    username = sys.argv[1]
    password = sys.argv[2]
    
    try:
        results = scrape_quizzes(username, password)
        print(json.dumps(results, ensure_ascii=False))
    except Exception as e:
        sys.exit(1)
