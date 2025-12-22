// 공휴일 API 서비스
class HolidayService {
  constructor() {
    // 한국천문연구원 특일정보 API 사용
    this.apiUrl = 'http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService';
    this.serviceKey = import.meta.env.VITE_HOLIDAY_API_KEY || 'YOUR_API_KEY';
  }

  // 특정 연도의 공휴일 조회
  async getHolidays(year) {
    try {
      const response = await fetch(
        `${this.apiUrl}/getRestDeInfo?serviceKey=${this.serviceKey}&solYear=${year}&_type=json`
      );
      const data = await response.json();
      
      if (data.response?.body?.items?.item) {
        return data.response.body.items.item.map(item => ({
          date: item.locdate.toString(),
          name: item.dateName
        }));
      }
      return [];
    } catch (error) {
      console.error('공휴일 조회 실패:', error);
      // API 실패 시 기본 공휴일 반환
      return this.getDefaultHolidays(year);
    }
  }

  // API 실패 시 기본 공휴일 (주요 공휴일만)
  getDefaultHolidays(year) {
    return [
      { date: `${year}0101`, name: '신정' },
      { date: `${year}0301`, name: '삼일절' },
      { date: `${year}0505`, name: '어린이날' },
      { date: `${year}0606`, name: '현충일' },
      { date: `${year}0815`, name: '광복절' },
      { date: `${year}1003`, name: '개천절' },
      { date: `${year}1009`, name: '한글날' },
      { date: `${year}1225`, name: '크리스마스' }
    ];
  }

  // 날짜가 공휴일인지 확인
  isHoliday(date, holidays) {
    const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '');
    return holidays.some(holiday => holiday.date === dateStr);
  }

  // 주말인지 확인 (토요일: 6, 일요일: 0)
  isWeekend(date) {
    const day = date.getDay();
    return day === 0 || day === 6;
  }

  // 영업일인지 확인 (주말, 공휴일 제외)
  isBusinessDay(date, holidays) {
    return !this.isWeekend(date) && !this.isHoliday(date, holidays);
  }

  // 수강권 만료일 계산 (영업일 기준)
  async calculateEndDate(startDate, weeks) {
    const start = new Date(startDate);
    const currentYear = start.getFullYear();
    const nextYear = currentYear + 1;
    
    // 시작년도와 다음년도 공휴일 조회
    const [currentYearHolidays, nextYearHolidays] = await Promise.all([
      this.getHolidays(currentYear),
      this.getHolidays(nextYear)
    ]);
    
    const allHolidays = [...currentYearHolidays, ...nextYearHolidays];
    
    let businessDaysCount = 0;
    let currentDate = new Date(start);
    const targetBusinessDays = weeks * 5; // 주 5일 기준
    
    while (businessDaysCount < targetBusinessDays) {
      if (this.isBusinessDay(currentDate, allHolidays)) {
        businessDaysCount++;
      }
      
      if (businessDaysCount < targetBusinessDays) {
        currentDate.setDate(currentDate.getDate() + 1);
      }
    }
    
    return currentDate;
  }

  // 남은 영업일 계산
  async calculateRemainingBusinessDays(startDate, endDate) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const today = new Date();
    
    if (today > end) return 0;
    
    const currentYear = today.getFullYear();
    const nextYear = currentYear + 1;
    
    const [currentYearHolidays, nextYearHolidays] = await Promise.all([
      this.getHolidays(currentYear),
      this.getHolidays(nextYear)
    ]);
    
    const allHolidays = [...currentYearHolidays, ...nextYearHolidays];
    
    let businessDaysCount = 0;
    let currentDate = new Date(Math.max(today, start));
    
    while (currentDate <= end) {
      if (this.isBusinessDay(currentDate, allHolidays)) {
        businessDaysCount++;
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return businessDaysCount;
  }
}

export const holidayService = new HolidayService();
