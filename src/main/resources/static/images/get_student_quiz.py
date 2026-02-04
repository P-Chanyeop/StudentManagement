#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
import json
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
import time

# Windows 인코딩 문제 해결
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

def scrape_quizzes(username, password):
    options = Options()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    
    driver = webdriver.Chrome(options=options)
    driver.implicitly_wait(10)
    
    try:
        # 로그인
        driver.get('https://global-zone60.renaissance-go.com/studentportal/entry')
        
        try:
            rpid_input = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.ID, "TenantRPID"))
            )
            rpid_input.send_keys('RPNA78NJ')
            driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
            time.sleep(2)
        except:
            pass
        
        username_field = WebDriverWait(driver, 15).until(
            EC.element_to_be_clickable((By.ID, "Username"))
        )
        username_field.send_keys(username)
        
        password_field = driver.find_element(By.ID, "Password")
        password_field.send_keys(password)
        
        driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
        time.sleep(5)
        
        # 퀴즈 링크 수집
        quiz_links = driver.find_elements(By.CSS_SELECTOR, "a.title-link")
        total_quizzes = len(quiz_links)
        
        results = []
        
        for i in range(total_quizzes):
            try:
                quiz_links = driver.find_elements(By.CSS_SELECTOR, "a.title-link")
                link = quiz_links[i]
                title_text = link.text
                
                # 퀴즈 클릭
                link.click()
                
                # View TOPS Report 버튼 클릭
                tops_button = WebDriverWait(driver, 15).until(
                    EC.element_to_be_clickable((By.CSS_SELECTOR, "button.tops-button"))
                )
                tops_button.click()
                
                # TOPS Report 페이지 로딩 대기
                WebDriverWait(driver, 15).until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, ".book-article-span-header"))
                )
                
                quiz_data = {'title': title_text}
                
                # Class 정보
                try:
                    class_spans = driver.find_elements(By.CSS_SELECTOR, ".flex-top-column")
                    for col in class_spans:
                        if "Class" in col.text:
                            value = col.find_element(By.CSS_SELECTOR, ".span-value").text.strip()
                            quiz_data['class'] = value
                            break
                except:
                    quiz_data['class'] = 'N/A'
                
                # Book 제목
                try:
                    quiz_data['bookTitle'] = driver.find_element(By.CSS_SELECTOR, ".book-article-span-header").text.strip()
                except:
                    quiz_data['bookTitle'] = 'N/A'
                
                # Book 상세 정보
                try:
                    book_lis = driver.find_elements(By.CSS_SELECTOR, ".book-article-list li")
                    for li in book_lis:
                        text = li.text
                        try:
                            if "Author" in text:
                                quiz_data['author'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "ATOS Book Level" in text:
                                quiz_data['atosLevel'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "Quiz #" in text:
                                quiz_data['quizNumber'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "Quiz Date" in text:
                                quiz_data['quizDate'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "Interest Level" in text:
                                quiz_data['interestLevel'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "TWI" in text:
                                quiz_data['twi'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "Type" in text:
                                quiz_data['type'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "Word Count" in text:
                                quiz_data['wordCount'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                        except:
                            pass
                except:
                    pass
                
                # Quiz Results
                try:
                    quiz_data['quizResult'] = driver.find_element(By.CSS_SELECTOR, ".quiz-results-span-header").text.strip()
                except:
                    quiz_data['quizResult'] = 'N/A'
                
                try:
                    result_lis = driver.find_elements(By.CSS_SELECTOR, ".quiz-result-list li")
                    for li in result_lis:
                        text = li.text
                        try:
                            if "% Correct" in text:
                                quiz_data['percentCorrect'] = li.find_element(By.CSS_SELECTOR, ".bold-text").text.strip()
                            elif "Points Earned" in text:
                                quiz_data['pointsEarned'] = li.find_element(By.TAG_NAME, "strong").text.strip()
                        except:
                            pass
                except:
                    pass
                
                # School Year Summary
                try:
                    summary = {}
                    # 학년도 기간
                    try:
                        summary['period'] = driver.find_element(By.CSS_SELECTOR, ".school-year-progress").text.strip()
                    except:
                        pass
                    # 각 항목
                    summary_lis = driver.find_elements(By.CSS_SELECTOR, ".style-year-summary-content li")
                    for li in summary_lis:
                        try:
                            label = li.find_element(By.CSS_SELECTOR, ".style-content-label").text.strip()
                            value = li.find_element(By.CSS_SELECTOR, ".bold").text.strip()
                            summary[label] = value
                        except:
                            pass
                    quiz_data['yearSummary'] = summary
                except:
                    quiz_data['yearSummary'] = {}
                
                # 기본값
                for key in ['author', 'atosLevel', 'quizNumber', 'quizDate', 'interestLevel', 'twi', 'type', 'wordCount', 'percentCorrect', 'pointsEarned']:
                    if key not in quiz_data:
                        quiz_data[key] = 'N/A'
                
                results.append(quiz_data)
                
                # 메인 페이지로
                driver.get('https://global-zone60.renaissance-go.com/studentportal/')
                time.sleep(2)
                
            except Exception as e:
                results.append({'title': title_text, 'error': str(e)})
                try:
                    driver.get('https://global-zone60.renaissance-go.com/studentportal/')
                    time.sleep(2)
                except:
                    pass
        
        return results
        
    finally:
        driver.quit()

if __name__ == '__main__':
    if len(sys.argv) < 3:
        sys.exit(1)
    
    username = sys.argv[1]
    password = sys.argv[2]
    
    try:
        results = scrape_quizzes(username, password)
        print(json.dumps(results, ensure_ascii=False))
    except:
        sys.exit(1)
