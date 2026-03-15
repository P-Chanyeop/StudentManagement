import { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import {
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { authAPI, userMenuAPI } from '../services/api';
import '../styles/Sidebar.css';

function Sidebar() {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [sidebarTop, setSidebarTop] = useState(window.innerHeight / 2);
  const [isEditMode, setIsEditMode] = useState(false);
  const [customMenuOrder, setCustomMenuOrder] = useState([]);
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    document.body.style.overflow = isMobileOpen ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  }, [isMobileOpen]);
  const queryClient = useQueryClient();

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 사용자 메뉴 순서 조회
  const { data: userMenuOrder } = useQuery({
    queryKey: ['userMenuOrder'],
    queryFn: async () => {
      const response = await userMenuAPI.getMenuOrder();
      return response.data;
    },
    enabled: !!profile,
  });

  // 메뉴 순서 저장
  const saveMenuOrderMutation = useMutation({
    mutationFn: (menuPaths) => userMenuAPI.saveMenuOrder(menuPaths),
    onSuccess: () => {
      queryClient.invalidateQueries(['userMenuOrder']);
      setIsEditMode(false);
    },
  });

  useEffect(() => {
    let timeoutId;
    
    const handleScroll = () => {
      // 기존 타이머 클리어
      clearTimeout(timeoutId);
      
      // 0.05초 후에 현재 화면 정중앙으로 이동
      timeoutId = setTimeout(() => {
        const newTop = window.scrollY + (window.innerHeight / 2);
        setSidebarTop(newTop);
      }, 50);
    };

    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
      clearTimeout(timeoutId);
    };
  }, []);

  const handleLogout = () => {
    if (window.confirm('로그아웃 하시겠습니까?')) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      navigate('/login');
    }
  };

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  // 역할별 메뉴 아이템 정의
  const getMenuItems = () => {
    const adminMenus = [
      { path: '/dashboard', icon: <i className="fas fa-chart-bar"></i>, label: '대시보드' },
      { path: '/students', icon: <i className="fas fa-users"></i>, label: '학생 관리' },
      { path: '/teachers', icon: <i className="fas fa-user-tie"></i>, label: '선생님 관리' },
      { path: '/courses', icon: <i className="fas fa-chalkboard-teacher"></i>, label: '반 관리' },
      // { path: '/class-info', icon: <i className="fas fa-calendar-check"></i>, label: '수업 스케줄' },
      { path: '/check-in', icon: <i className="fas fa-check-circle"></i>, label: '출석 체크' },
      { path: '/attendance', icon: <i className="fas fa-clipboard-list"></i>, label: '출석부' },
      { path: '/additional-class', icon: <i className="fas fa-plus-circle"></i>, label: '추가수업' },
      { path: '/reservations', icon: <i className="fas fa-calendar-alt"></i>, label: '예약 관리' },
      { path: '/parent-reservation', icon: <i className="fas fa-calendar-plus"></i>, label: '수업 예약' },
      { path: '/enrollments', icon: <i className="fas fa-ticket-alt"></i>, label: '수강권 관리' },
      // { path: '/enrollment-adjustment', icon: <i className="fas fa-edit"></i>, label: '횟수 조정' },
      { path: '/consultations', icon: <i className="fas fa-chart-line"></i>, label: '학습 현황' },
      { path: '/quiz-management', icon: <i className="fas fa-book-reader"></i>, label: '퀴즈 관리' },
      { path: '/messages', icon: <i className="fas fa-envelope"></i>, label: '문자 발송' },
      { path: '/notices', icon: <i className="fas fa-bell"></i>, label: '공지사항' },
    ];

    const teacherMenus = [
      { path: '/dashboard', icon: <i className="fas fa-chart-bar"></i>, label: '대시보드' },
      { path: '/students', icon: <i className="fas fa-users"></i>, label: '학생 관리' },
      { path: '/courses', icon: <i className="fas fa-chalkboard-teacher"></i>, label: '반 관리' },
      // { path: '/class-info', icon: <i className="fas fa-calendar-check"></i>, label: '수업 스케줄' },
      { path: '/check-in', icon: <i className="fas fa-check-circle"></i>, label: '출석 체크' },
      { path: '/attendance', icon: <i className="fas fa-clipboard-list"></i>, label: '출석부' },
      { path: '/additional-class', icon: <i className="fas fa-plus-circle"></i>, label: '추가수업' },
      { path: '/reservations', icon: <i className="fas fa-calendar-alt"></i>, label: '예약 관리' },
      { path: '/parent-reservation', icon: <i className="fas fa-calendar-plus"></i>, label: '수업 예약' },
      { path: '/consultation-reservation', icon: <i className="fas fa-user-md"></i>, label: '상담 예약' },
      { path: '/enrollments', icon: <i className="fas fa-ticket-alt"></i>, label: '수강권 관리' },
      // { path: '/enrollment-adjustment', icon: <i className="fas fa-edit"></i>, label: '횟수 조정' },
      { path: '/consultations', icon: <i className="fas fa-chart-line"></i>, label: '학습 현황' },
      { path: '/messages', icon: <i className="fas fa-envelope"></i>, label: '문자 발송' },
      { path: '/notices', icon: <i className="fas fa-bell"></i>, label: '공지사항' },
    ];

    const studentParentMenus = [
      { path: '/dashboard', icon: <i className="fas fa-chart-bar"></i>, label: '대시보드' },
      { path: '/notices', icon: <i className="fas fa-bell"></i>, label: '공지사항' },
      { path: '/students', icon: <i className="fas fa-user"></i>, label: '자녀 관리' },
      { path: '/class-info', icon: <i className="fas fa-calendar-check"></i>, label: '수업 정보' },
      { path: '/parent-reservation', icon: <i className="fas fa-calendar-plus"></i>, label: '수업 예약' },
      { path: '/consultation-reservation', icon: <i className="fas fa-user-md"></i>, label: '상담 예약' },
      { path: '/reservations', icon: <i className="fas fa-calendar-alt"></i>, label: '예약 내역' },
      { path: '/my-quiz', icon: <i className="fas fa-book-reader"></i>, label: '영어 퀴즈' },
      { path: '/consultations', icon: <i className="fas fa-chart-line"></i>, label: '학습 현황' },
    ];

    if (profile?.role === 'PARENT') {
      return studentParentMenus;
    } else if (profile?.role === 'TEACHER') {
      return teacherMenus;
    } else if (profile?.role === 'ADMIN') {
      return adminMenus;
    }
    return adminMenus;
  };

  const menuItems = getMenuItems();

  // 사용자 정의 순서로 메뉴 정렬
  const getOrderedMenuItems = () => {
    if (!userMenuOrder?.menuPaths || userMenuOrder.menuPaths.length === 0) {
      return menuItems;
    }

    const orderedItems = [];
    const remainingItems = [...menuItems];

    // 저장된 순서대로 메뉴 추가
    userMenuOrder.menuPaths.forEach(path => {
      const item = remainingItems.find(menu => menu.path === path);
      if (item) {
        orderedItems.push(item);
        const index = remainingItems.indexOf(item);
        remainingItems.splice(index, 1);
      }
    });

    // 남은 메뉴들 추가 (새로 추가된 메뉴)
    return [...orderedItems, ...remainingItems];
  };

  const orderedMenuItems = isEditMode ? 
    (customMenuOrder.length > 0 ? customMenuOrder : getOrderedMenuItems()) : 
    getOrderedMenuItems();

  // 편집 모드 시작
  const startEditMode = () => {
    setIsEditMode(true);
  };

  // 편집 모드가 시작될 때 customMenuOrder 설정
  useEffect(() => {
    if (isEditMode && customMenuOrder.length === 0) {
      setCustomMenuOrder([...getOrderedMenuItems()]);
    }
  }, [isEditMode]);

  // 편집 취소 및 저장
  const cancelEdit = () => {
    // X 버튼 클릭 시 저장하고 편집 모드 종료
    const menuPaths = customMenuOrder.map(item => item.path);
    saveMenuOrderMutation.mutate(menuPaths);
    setIsEditMode(false);
    setCustomMenuOrder([]);
  };

  // 드래그 앤 드롭 핸들러 (@dnd-kit)
  const handleDragEnd = (event) => {
    const { active, over } = event;

    if (active.id !== over?.id) {
      setCustomMenuOrder((items) => {
        const oldIndex = items.findIndex((item) => item.path === active.id);
        const newIndex = items.findIndex((item) => item.path === over.id);

        return arrayMove(items, oldIndex, newIndex);
      });
    }
  };

  return (
    <>
      <button className="mobile-hamburger" onClick={() => setIsMobileOpen(!isMobileOpen)}>
        <i className={`fas ${isMobileOpen ? 'fa-times' : 'fa-bars'}`}></i>
      </button>
      {isMobileOpen && <div className="mobile-overlay" onClick={() => setIsMobileOpen(false)} />}
      <div 
        className={`sidebar ${isCollapsed ? 'collapsed' : ''} ${isMobileOpen ? 'mobile-open' : ''}`}
        style={{
          top: `${sidebarTop}px`
        }}
      >
      <button className="toggle-btn" onClick={toggleSidebar}>
        <i className={`fas ${isCollapsed ? 'fa-chevron-right' : 'fa-chevron-left'}`}></i>
      </button>
      
      <div className="sidebar-header">
        <div className="logo">
          <span className="logo-icon">🎓</span>
          {!isCollapsed && <h2>{profile?.role === 'PARENT' ? '자녀 관리 시스템' : '학원 관리 시스템'}</h2>}
        </div>
        {!isCollapsed && (
          <div className="menu-edit-controls">
            {!isEditMode ? (
              <button className="edit-menu-btn" onClick={startEditMode} title="메뉴 순서 편집">
                <i className="fas fa-edit"></i>
              </button>
            ) : (
              <div className="edit-controls">
                <button className="cancel-btn" onClick={cancelEdit} title="저장하고 닫기">
                  <i className="fas fa-times"></i>
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      <nav className="sidebar-nav">
        {isEditMode ? (
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={orderedMenuItems.map(item => item.path)}
              strategy={verticalListSortingStrategy}
            >
              {orderedMenuItems.map((item) => (
                <SortableItem
                  key={item.path}
                  item={item}
                  isCollapsed={isCollapsed}
                />
              ))}
            </SortableContext>
          </DndContext>
        ) : (
          orderedMenuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              title={isCollapsed ? item.label : ''}
              onClick={() => setIsMobileOpen(false)}
            >
              <span className="nav-icon">{item.icon}</span>
              {!isCollapsed && <span className="nav-label">{item.label}</span>}
            </NavLink>
          ))
        )}
      </nav>

      <div className="sidebar-footer">
        <button className="logout-btn" onClick={handleLogout} title={isCollapsed ? '로그아웃' : ''}>
          <span className="nav-icon"><i className="fas fa-sign-out-alt"></i></span>
          {!isCollapsed && <span className="nav-label">로그아웃</span>}
        </button>
      </div>
    </div>
    </>
  );
}

// SortableItem 컴포넌트
function SortableItem({ item, isCollapsed }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.path });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className={`nav-item draggable ${isDragging ? 'dragging' : ''}`}
    >
      <span className="nav-icon">{item.icon}</span>
      {!isCollapsed && <span className="nav-label">{item.label}</span>}
      {!isCollapsed && (
        <span className="drag-handle">
          <i className="fas fa-grip-vertical"></i>
        </span>
      )}
    </div>
  );
}

export default Sidebar;
