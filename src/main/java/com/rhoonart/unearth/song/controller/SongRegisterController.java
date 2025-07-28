package com.rhoonart.unearth.song.controller;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.right_holder.service.RightHolderUtilService;
import com.rhoonart.unearth.song.dto.SongBulkRegisterResponseDto;
import com.rhoonart.unearth.song.dto.SongBulkRegisterResultDto;
import com.rhoonart.unearth.song.dto.SongInfoRegisterRequestDto;
import com.rhoonart.unearth.song.service.SongBulkRegisterService;
import com.rhoonart.unearth.song.service.SongInfoService;
import com.rhoonart.unearth.song.service.SongRegisterService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
@RequestMapping("/song")
@RequiredArgsConstructor
public class SongRegisterController {

    private final SongBulkRegisterService songBulkRegisterService;
    private final RightHolderUtilService rightHolderUtilService;
    private final SongRegisterService songRegisterService;

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute SongInfoRegisterRequestDto dto,
                           HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        songRegisterService.register(dto);
        return "redirect:/song/list";
    }

    /**
     * CSV 일괄 등록 페이지
     */
    @GetMapping("/bulk-register")
    public String bulkRegisterPage(Model model, HttpSession session) {
        SessionUserUtil.requireAdminRole(session);

        // 권리자 목록 (등록된 권리자 확인용)
        List<String> rightHolders = rightHolderUtilService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);

        return "song/bulk-register";
    }

    /**
     * CSV 파일 업로드 및 일괄 등록 처리 (AJAX용)
     */
    @PostMapping("/bulk-register-ajax")
    @ResponseBody
    public CommonResponse<SongBulkRegisterResponseDto> bulkRegisterAjax(@RequestParam("file") MultipartFile file,
                                              HttpSession session) {
        SessionUserUtil.requireAdminRole(session);

        try {
            // 파일 유효성 검사
            if (file.isEmpty()) {
                return CommonResponse.fail(ResponseCode.INVALID_INPUT, "업로드된 파일이 비어있습니다.");
            }

            if (!isValidCsvFile(file)) {
                return CommonResponse.fail(ResponseCode.INVALID_INPUT, "CSV 파일만 업로드 가능합니다.");
            }

            // 일괄 등록 실행
            return CommonResponse.success(songBulkRegisterService.bulkRegisterFromCsv(file));

        } catch (Exception e) {
            log.error("❌ CSV 일괄 등록 실패", e);

            String errorMessage = "일괄 등록 중 오류가 발생했습니다.";

            // 구체적인 오류 메시지 추출
            if (e instanceof BaseException) {
                errorMessage = e.getMessage();
            } else if (e.getCause() instanceof BaseException) {
                errorMessage = e.getCause().getMessage();
            } else if (e.getMessage() != null) {
                // 일반적인 예외의 경우에도 메시지가 있으면 사용
                errorMessage = e.getMessage();
            }

            return CommonResponse.fail(ResponseCode.SERVER_ERROR, errorMessage);
        }
    }

    /**
     * CSV 파일 유효성 검사
     */
    private boolean isValidCsvFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        // 파일 확장자 검사
        return originalFilename.toLowerCase().endsWith(".csv");
    }
}
