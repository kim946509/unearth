package com.rhoonart.unearth.right_holder.controller;

import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.right_holder.service.RightHolderUpdateService;
import com.rhoonart.unearth.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.servlet.http.HttpSession;
import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderUpdateRequestDto;
import com.rhoonart.unearth.right_holder.dto.ContractExtendRequestDto;
import com.rhoonart.unearth.right_holder.dto.LoginToggleRequestDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/right-holder")
@RequiredArgsConstructor
public class RightHolderController {
    private final RightHolderService rightHolderService;
    private final RightHolderUpdateService rightHolderUpdateService;

    /**
     * 권리자 목록 페이지
     * @param holderTypeStr : HolderType의 이름으로 필터링 (예: "개인", "사업자")
     * @param holderName : 권리자 이름으로 필터링
     * @param contractDateStr : 해당 날짜로부터 계약 기간이 포함된 권리자만 조회
     * @param page : 페이지 번호 (0부터 시작)
     * @param size : 페이지 크기 (10, 30, 50 중 하나)
     * @param session : HTTP 세션, 사용자 권한 확인에 사용
     * @param model : Spring MVC 모델, 뷰에 데이터를 전달하는 데 사용
     * @return
     */
    @GetMapping("/list")
    public String listPage(
            @RequestParam(value = "holderType", required = false) String holderTypeStr,
            @RequestParam(value = "holderName", required = false) String holderName,
            @RequestParam(value = "contractDate", required = false) String contractDateStr,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);


        Page<RightHolderListResponseDto> holderPage = rightHolderService.findRightHolders(holderTypeStr, contractDateStr, holderName,
                page, size);
        CommonResponse<Page<RightHolderListResponseDto>> response = CommonResponse.success(holderPage);
        model.addAttribute("response", response);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("user", user); // 뷰에서 사용자 정보 활용 가능
        model.addAttribute("holderType", holderTypeStr);
        model.addAttribute("holderName", holderName);
        model.addAttribute("contractDate", contractDateStr);
        return "right_holder/list";
    }

    /**
     * 권리자 등록 페이지
     * @param dto : 권리자 등록 요청 DTO
     * @param session : HTTP 세션, 사용자 권한 확인에 사용
     * @param model : Spring MVC 모델, 뷰에 데이터를 전달하는 데 사용
     * @return : 권리자 리스트 페이지로 리다이렉트
     */
    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute RightHolderRegisterRequestDto dto,
            HttpSession session,
            Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);
        rightHolderService.register(dto);
        // 등록 성공 시 목록으로 이동
        return "redirect:/right-holder/list";
    }

    /**
     * 권리자 수정 페이지
     * @param rightHolderId : 수정할 권리자의 ID
     * @param dto : 권리자 수정 요청 DTO
     * @param session : HTTP 세션, 사용자 권한 확인에 사용
     * @param model : Spring MVC 모델, 뷰에 데이터를 전달하는 데 사용
     * @return : 권리자 리스트 페이지로 리다이렉트
     */
    @PostMapping("/{rightHolderId}/update")
    public String updateSubmit(@PathVariable String rightHolderId,
            @Valid @ModelAttribute RightHolderUpdateRequestDto dto,
            HttpSession session,
            Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);
        rightHolderService.update(rightHolderId, dto);
        // 수정 성공 시 목록으로 이동
        return "redirect:/right-holder/list";
    }

    /**
     * 권리자 계약 연장
     * @param rightHolderId : 연장할 권리자의 ID
     * @param dto : 계약 연장 요청 DTO
     * @param session : HTTP 세션, 사용자 권한 확인에 사용
     * @return : 계약 연장 성공 메시지를 포함한 CommonResponse
     */
    @PostMapping("/{rightHolderId}/extend")
    @ResponseBody
    public CommonResponse<String> extendContract(
            @PathVariable String rightHolderId,
            @Valid @RequestBody ContractExtendRequestDto dto,
            HttpSession session) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);

        rightHolderUpdateService.extendContract(rightHolderId, dto.getNewEndDate());
        return CommonResponse.success("계약이 성공적으로 연장되었습니다.");
    }

    /**
     * 권리자의 로그인 가능 여부를 활성화/비활성화하는 컨트롤러
     * @param rightHolderId : 로그인 상태를 변경할 권리자의 ID
     * @param dto : 로그인 토글 요청 DTO
     * @param session : HTTP 세션, 사용자 권한 확인에 사용
     * @return
     */
    @PostMapping("/{rightHolderId}/toggle-login")
    @ResponseBody
    public CommonResponse<String> toggleLoginStatus(
            @PathVariable String rightHolderId,
            @Valid @RequestBody LoginToggleRequestDto dto,
            HttpSession session) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);

        rightHolderService.rightHolderLoginStatusUpdate(rightHolderId, dto.getIsLoginEnabledAsBoolean());
        String action = dto.getIsLoginEnabledAsBoolean() ? "활성화" : "비활성화";
        return CommonResponse.success("로그인이 성공적으로 " + action + "되었습니다.");
    }
}