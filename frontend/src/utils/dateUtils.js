// 한국 시간(UTC+9) 기준 날짜 유틸리티

/**
 * 로컬 날짜를 YYYY-MM-DD 형식으로 반환
 */
export const getLocalDateString = (date = new Date()) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

/**
 * 오늘 날짜를 YYYY-MM-DD 형식으로 반환
 */
export const getTodayString = () => getLocalDateString(new Date());

/**
 * N일 후 날짜를 YYYY-MM-DD 형식으로 반환
 */
export const getDateAfterDays = (days) => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return getLocalDateString(date);
};
