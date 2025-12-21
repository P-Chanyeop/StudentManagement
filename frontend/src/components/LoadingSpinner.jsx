import '../styles/LoadingSpinner.css';

function LoadingSpinner() {
  return (
    <div className="loading-container">
      <div className="sun-loader">
        <div className="ray ray-1"></div>
        <div className="ray ray-2"></div>
        <div className="ray ray-3"></div>
        <div className="ray ray-4"></div>
        <div className="ray ray-5"></div>
        <div className="ray ray-6"></div>
        <div className="ray ray-7"></div>
        <div className="ray ray-8"></div>
        <div className="center-circle"></div>
      </div>
      <p className="loading-text">로딩 중...</p>
    </div>
  );
}

export default LoadingSpinner;
