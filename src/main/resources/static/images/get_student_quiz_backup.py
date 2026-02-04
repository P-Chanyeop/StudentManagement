#!/usr/bin/env python3
"""
르네상스 퀴즈 스크래핑
"""
import sys
import json
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options

def scrape_quizzes(username, password):
    options = Options()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    
    driver = webdriver.Chrome(options=options)
    
    try:
        print(f"로그인 중: {username}...")
        
        # 로그인
        driver.get('https://global-zone60.renaissance-go.com/studentportal/entry')
        
        # Tenant
        try:
            rpid_input = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.ID, "TenantRPID"))
            )
            rpid_input.send_keys('RPNA78NJ')
            driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
            import time
            time.sleep(2)
        except:
            pass
        
        # Login
        username_field = WebDriverWait(driver, 15).until(
            EC.element_to_be_clickable((By.ID, "Username"))
        )
        username_field.send_keys(username)
        
        password_field = driver.find_element(By.ID, "Password")
        password_field.send_keys(password)
        
        driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
        
        # 로그인 완료 대기
        import time
        time.sleep(5)
        
        print("✓ 로그인 성공!")
        print(f"Current URL: {driver.current_url}")
        
        # 1. 퀴즈 링크 수집
        print("\n퀴즈 링크 수집 중...")
        quiz_links = driver.find_elements(By.CSS_SELECTOR, "a.title-link")
        
        print(f"찾은 퀴즈: {len(quiz_links)}개")
        
        # 2. 각 퀴즈 클릭해서 정보 가져오기
        results = []
        total_quizzes = len(quiz_links)
        
        for i in range(total_quizzes):  # 모든 퀴즈
            try:
                # 링크 다시 찾기
                quiz_links = driver.find_elements(By.CSS_SELECTOR, "a.title-link")
                if i >= len(quiz_links):
                    break
                
                link = quiz_links[i]
                title_text = link.text
                print(f"\n[{i+1}/{total_quizzes}] {title_text}")
                
                # 클릭
                link.click()
                time.sleep(3)
                
                # 책 제목 가져오기
                try:
                    book_title_elem = WebDriverWait(driver, 5).until(
                        EC.presence_of_element_located((By.CSS_SELECTOR, ".book-title"))
                    )
                    book_title = book_title_elem.text
                    
                    # 모든 정보 수집
                    quiz_data = {
                        'username': username,
                        'title': title_text,
                        'bookTitle': book_title
                    }
                    
                    # 페이지의 모든 요소 출력 (디버깅) - 제거
                    # if i == 0:
                    #     ...
                    
                    
                    # 점수
                    try:
                        score_elem = driver.find_element(By.CSS_SELECTOR, ".number-correct-emphasis")
                        quiz_data['percentCorrect'] = score_elem.text.strip()
                    except:
                        quiz_data['percentCorrect'] = 'N/A'
                    
                    # 날짜 (여러 셀렉터 시도)
                    try:
                        date_elem = driver.find_element(By.CSS_SELECTOR, ".completion-date, .date-taken, .quiz-date")
                        quiz_data['dateTaken'] = date_elem.text.strip()
                    except:
                        # 텍스트에서 날짜 패턴 찾기
                        try:
                            import re
                            page_text = driver.find_element(By.TAG_NAME, "body").text
                            date_match = re.search(r'(\d{1,2}/\d{1,2}/\d{4})', page_text)
                            quiz_data['dateTaken'] = date_match.group(1) if date_match else 'N/A'
                        except:
                            quiz_data['dateTaken'] = 'N/A'
                    
                    # 난이도
                    try:
                        level_elem = driver.find_element(By.CSS_SELECTOR, ".book-level, .atos-level")
                        import re
                        level_text = level_elem.text
                        level_match = re.search(r'(\d+\.\d+)', level_text)
                        quiz_data['atosLevel'] = level_match.group(1) if level_match else level_text.strip()
                    except:
                        quiz_data['atosLevel'] = 'N/A'
                    
                    # 포인트
                    try:
                        points_elem = driver.find_element(By.CSS_SELECTOR, ".points-earned, [class*='point']")
                        import re
                        points_text = points_elem.text
                        points_match = re.search(r'([\d.]+)', points_text)
                        quiz_data['pointsEarned'] = points_match.group(1) if points_match else points_text.strip()
                    except:
                        quiz_data['pointsEarned'] = 'N/A'
                    
                    # 단어 수
                    try:
                        words_elem = driver.find_element(By.CSS_SELECTOR, "[class*='word']")
                        quiz_data['wordCount'] = words_elem.text
                    except:
                        quiz_data['wordCount'] = 'N/A'
                    
                    print(f"  책 제목: {book_title}")
                    print(f"  점수: {quiz_data['percentCorrect']}")
                    print(f"  난이도: {quiz_data['atosLevel']}")
                    print(f"  포인트: {quiz_data['pointsEarned']}")
                    print(f"  단어 수: {quiz_data['wordCount']}")
                    
                    results.append(quiz_data)
                except Exception as e:
                    print(f"  정보 수집 실패: {e}")
                    results.append({
                        'title': title_text,
                        'bookTitle': 'N/A',
                        'error': str(e)
                    })
                
                # 뒤로 가기
                driver.back()
                time.sleep(2)
                
            except Exception as e:
                print(f"  에러: {e}")
        
        return results
        
    finally:
        driver.quit()

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: python scrape_quizzes.py <username> <password>")
        sys.exit(1)
    
    username = sys.argv[1]
    password = sys.argv[2]
    
    try:
        results = scrape_quizzes(username, password)
        
        print(f"\n\n=== 결과 ===")
        for item in results:
            print(f"- {item['bookTitle']}")
        
        print("\n" + json.dumps(results, ensure_ascii=False, indent=2))
        
    except Exception as e:
        print(f"✗ 실패: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
