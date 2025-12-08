//package com.moa.service.user.impl;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyBoolean;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import com.moa.common.exception.BusinessException;
//import com.moa.dao.user.EmailVerificationDao;
//import com.moa.dao.user.UserDao;
//import com.moa.domain.User;
//import com.moa.domain.enums.UserStatus;
//import com.moa.dto.user.request.DeleteUserRequest;
//import com.moa.dto.user.request.LoginRequest;
//import com.moa.dto.user.request.UserCreateRequest;
//import com.moa.dto.user.request.UserUpdateRequest;
//import com.moa.dto.user.response.LoginResponse;
//import com.moa.dto.user.response.UserResponse;
//import com.moa.service.mail.EmailService;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;
//
//class UserServiceImplTest {
//
//	@Mock
//	private UserDao userDao;
//
//	@Mock
//	private EmailVerificationDao emailVerificationDao;
//
//	@Mock
//	private PasswordEncoder passwordEncoder;
//
//	@Mock
//	private EmailService emailService;
//
//	// 세션 Mocking 필드
//	@Mock
//	private ServletRequestAttributes servletRequestAttributes;
//	@Mock
//	private HttpServletRequest httpServletRequest;
//	@Mock
//	private HttpSession httpSession;
//
//	private MockedStatic<RequestContextHolder> mockedRequestContext;
//
//	@InjectMocks
//	private UserServiceImpl userService;
//
//	@BeforeEach
//	void init() {
//		MockitoAnnotations.openMocks(this);
//
//		ReflectionTestUtils.setField(userService, "profileUploadDir", "uploads/");
//		ReflectionTestUtils.setField(userService, "profileUrlPrefix", "/uploads/");
//
//		// 세션/RequestContextHolder Mocking 설정
//		when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
//		when(httpServletRequest.getSession(anyBoolean())).thenReturn(httpSession);
//
//		mockedRequestContext = Mockito.mockStatic(RequestContextHolder.class);
//		mockedRequestContext.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
//	}
//
//	@AfterEach
//	void tearDown() {
//		if (mockedRequestContext != null) {
//			mockedRequestContext.close();
//		}
//	}
//
//	// ============================================================
//	// 회원가입 (PASS + 프로필 이미지 Base64 포함)
//	// ============================================================
//
//	@Test
//	@DisplayName("회원가입 성공 - PASS CI/DI + 프로필이미지 없음")
//	void addUser_success_withoutProfile() {
//		UserCreateRequest req = new UserCreateRequest();
//		req.setUserId("test@naver.com");
//		req.setPassword("Abcd1234");
//		req.setPasswordConfirm("Abcd1234");
//		req.setNickname("tester");
//		req.setPhone("01012341234");
//		req.setCi("CI_VALUE_123");
//		req.setDi("DI_VALUE_123");
//
//		when(userDao.existsByUserId("test@naver.com")).thenReturn(0);
//		when(userDao.existsByNickname("tester")).thenReturn(0);
//		when(passwordEncoder.encode("Abcd1234")).thenReturn("ENCODED");
//
//		UserResponse res = userService.addUser(req);
//
//		assertEquals("test@naver.com", res.getUserId());
//		assertEquals("tester", res.getNickname());
//
//		verify(userDao).insertUser(any(User.class));
//	}
//
//	@Test
//	@DisplayName("회원가입 성공 - 프로필 이미지(Base64) 포함")
//	void addUser_success_withProfileBase64() {
//		UserCreateRequest req = new UserCreateRequest();
//		req.setUserId("upload@naver.com");
//		req.setPassword("Abcd1234");
//		req.setPasswordConfirm("Abcd1234");
//		req.setNickname("uploder");
//		req.setPhone("01022223333");
//		req.setCi("CI1234");
//		req.setDi("DI1234");
//		req.setProfileImageBase64("data:image/png;base64,ZmFrZUJ5dGVz");
//
//		when(userDao.existsByUserId("upload@naver.com")).thenReturn(0);
//		when(userDao.existsByNickname("uploder")).thenReturn(0);
//		when(passwordEncoder.encode("Abcd1234")).thenReturn("ENC_PW");
//
//		UserResponse res = userService.addUser(req);
//
//		assertEquals("upload@naver.com", res.getUserId());
//		verify(userDao).insertUser(any(User.class));
//	}
//
//	@Test
//	@DisplayName("회원가입 실패 - 이메일 중복")
//	void addUser_emailExists() {
//		UserCreateRequest req = new UserCreateRequest();
//		req.setUserId("dup@naver.com");
//		req.setPassword("Abcd1234");
//		req.setPasswordConfirm("Abcd1234");
//		req.setNickname("nick");
//		req.setCi("CI_D");
//		req.setDi("DI_D");
//
//		when(userDao.existsByUserId("dup@naver.com")).thenReturn(1);
//
//		assertThrows(BusinessException.class, () -> userService.addUser(req));
//	}
//
//	// ============================================================
//	// 로그인 테스트
//	// ============================================================
//
//	@Test
//	@DisplayName("로그인 성공")
//	void login_success() {
//		LoginRequest req = new LoginRequest();
//		req.setUserId("user@naver.com");
//		req.setPassword("pass");
//
//		User user = User.builder().userId("user@naver.com").password("ENC").status(UserStatus.ACTIVE).build();
//
//		when(userDao.findByUserIdIncludeDeleted("user@naver.com")).thenReturn(Optional.of(user));
//		when(passwordEncoder.matches("pass", "ENC")).thenReturn(true);
//
//		LoginResponse res = userService.login(req);
//
//		assertEquals("user@naver.com", res.getUserId());
//
//		verify(userDao).resetLoginFailCount("user@naver.com");
//		verify(userDao).updateLastLoginDate("user@naver.com");
//	}
//
//	@Test
//	@DisplayName("로그인 실패 - 비밀번호 불일치")
//	void login_wrongPassword() {
//		LoginRequest req = new LoginRequest();
//		req.setUserId("user@naver.com");
//		req.setPassword("wrong");
//
//		User user = User.builder().userId("user@naver.com").password("ENC").status(UserStatus.ACTIVE).build();
//
//		when(userDao.findByUserIdIncludeDeleted("user@naver.com")).thenReturn(Optional.of(user));
//		when(passwordEncoder.matches("wrong", "ENC")).thenReturn(false);
//		when(userDao.getFailCount("user@naver.com")).thenReturn(1);
//
//		assertThrows(BusinessException.class, () -> userService.login(req));
//	}
//
//	// ============================================================
//	// 사용자 정보 수정
//	// ============================================================
//
//	@Test
//	@DisplayName("사용자 정보 수정 성공")
//	void updateUser_success() {
//		UserUpdateRequest req = new UserUpdateRequest();
//		req.setNickname("newNick");
//		req.setPhone("01099998888");
//		req.setProfileImage("image.png");
//
//		User user = User.builder().userId("user@naver.com").nickname("oldNick").phone("01011112222").password("ENC")
//				.role("USER").status(UserStatus.ACTIVE).regDate(LocalDateTime.now()).ci("ci").loginFailCount(0).build();
//
//		when(userDao.findByUserId("user@naver.com")).thenReturn(Optional.of(user));
//		when(userDao.existsByNickname("newNick")).thenReturn(0);
//
//		UserResponse res = userService.updateUser("user@naver.com", req);
//
//		assertEquals("newNick", res.getNickname());
//		assertEquals("01099998888", res.getPhone());
//		verify(userDao).updateUserProfile(any(User.class));
//	}
//
//	// ============================================================
//	// 소프트 삭제
//	// ============================================================
//
//	@Test
//	@DisplayName("회원 삭제 성공 (soft delete)")
//	void deleteUser_success() {
//		User user = User.builder().userId("del@naver.com").status(UserStatus.ACTIVE).build();
//
//		when(userDao.findByUserId("del@naver.com")).thenReturn(Optional.of(user));
//
//		DeleteUserRequest req = new DeleteUserRequest();
//
//		userService.deleteCurrentUser("del@naver.com", req);
//
//		verify(userDao).softDeleteUser("del@naver.com", UserStatus.WITHDRAW, "USER_REQUEST", null);
//	}
//}