// 공휴일 API 서비스 (백엔드 프록시)
class HolidayService {
  // 특정 연도의 공휴일 조회
  async getHolidays(year) {
    try {
      const response = await fetch(`/api/holidays/year/${year}`);
      if (!response.ok) throw new Error('API error');
      const data = await response.json();
      return data.map(item => ({
        date: item.date.replace(/-/g, ''),
        name: item.name
      }));
    } catch (error) {
      console.error('공휴일 조회 실패:', error);
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
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const dateStr = `${year}${month}${day}`;
    return holidays.some(holiday => holiday.date === dateStr);
  }

  // 주말인지 확인 (일요일: 0만 제외)
  isWeekend(date) {
    return date.getDay() === 0;
  }

  // 영업일인지 확인 (주말, 공휴일 제외)
  isBusinessDay(date, holidays) {
    return !this.isWeekend(date) && !this.isHoliday(date, holidays);
  }

  // 수강권 만료일 계산 (캐시된 공휴일 데이터 사용)
  calculateEndDateWithCache(startDate, businessDays, holidays) {
    const start = new Date(startDate);
    let businessDaysCount = 0;
    let currentDate = new Date(start);
    
    if (this.isBusinessDay(currentDate, holidays)) {
      businessDaysCount = 1;
    }
    
    while (businessDaysCount < businessDays) {
      currentDate.setDate(currentDate.getDate() + 1);
      if (this.isBusinessDay(currentDate, holidays)) {
        businessDaysCount++;
      }
    }
    
    return currentDate;
  }

  // 수강권 만료일 계산 (영업일 기준)
  async calculateEndDate(startDate, weeks) {
    const start = new Date(startDate);
    const currentYear = start.getFullYear();
    const nextYear = currentYear + 1;
    
    const [currentYearHolidays, nextYearHolidays] = await Promise.all([
      this.getHolidays(currentYear),
      this.getHolidays(nextYear)
    ]);
    
    const allHolidays = [...currentYearHolidays, ...nextYearHolidays];
    
    let businessDaysCount = 0;
    let currentDate = new Date(start);
    const targetBusinessDays = weeks * 6;
    
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

  // 남은 영업일 계산 (캐시된 공휴일 데이터 사용)
  calculateRemainingBusinessDaysWithCache(startDate, endDate, holidays) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const today = new Date();
    
    if (today > end) return 0;
    
    let businessDaysCount = 0;
    let currentDate = new Date(Math.max(today, start));
    
    while (currentDate <= end) {
      if (this.isBusinessDay(currentDate, holidays)) {
        businessDaysCount++;
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return businessDaysCount;
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
