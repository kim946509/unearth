package com.rhoonart.unearth.song.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.song.dto.SongInfoRegisterRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoUpdateRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoWithCrawlingDto;
import com.rhoonart.unearth.song.dto.SongBulkRegisterResponseDto;
import com.rhoonart.unearth.song.service.SongInfoService;
import com.rhoonart.unearth.song.service.SongBulkRegisterService;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/song")
@RequiredArgsConstructor
public class SongInfoController {
    private final SongInfoService songInfoService;
    private final SongBulkRegisterService songBulkRegisterService;
    private final RightHolderService rightHolderService;

    @GetMapping("/list")
    public String listPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "isCrawlingActive", required = false) Boolean isCrawlingActive,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        // size 허용값 검증
        if (size != 10 && size != 30 && size != 50) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<SongInfoWithCrawlingDto> songPage = songInfoService.findSongsWithCrawling(search, pageable,
                isCrawlingActive);
        model.addAttribute("response", CommonResponse.success(songPage));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("isCrawlingActive", isCrawlingActive != null && isCrawlingActive);
        // 권리자 드롭다운용 목록
        var rightHolders = rightHolderService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);

        // 권리자가 없으면 경고 로그 출력 (디버깅용)
        if (rightHolders.isEmpty()) {
            System.out.println("⚠️ 등록된 권리자가 없습니다. 음원 등록을 위해서는 먼저 권리자를 등록해주세요.");
        } else {
            System.out.println("✅ 권리자 목록 로드 완료: " + rightHolders.size() + "개");
        }
        return "song/list";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute SongInfoRegisterRequestDto dto,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        songInfoService.register(dto);
        return "redirect:/song/list";
    }

    @PostMapping("/{songId}/update")
    public String update(@PathVariable String songId,
            @Valid @ModelAttribute SongInfoUpdateRequestDto dto,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        songInfoService.update(songId, dto);

        // redirect 파라미터가 있으면 해당 페이지로, 없으면 기본 음원 리스트로
        if (redirect != null && !redirect.trim().isEmpty()) {
            return "redirect:" + redirect;
        }
        return "redirect:/song/list";
    }

    /**
     * CSV 일괄 등록 페이지
     */
    @GetMapping("/bulk-register")
    public String bulkRegisterPage(Model model, HttpSession session) {
        SessionUserUtil.requireAdminRole(session);

        // 권리자 목록 (등록된 권리자 확인용)
        var rightHolders = rightHolderService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);

        return "song/bulk-register";
    }

    /**
     * CSV 파일 업로드 및 일괄 등록 처리
     */
    @PostMapping("/bulk-register")
    public String bulkRegister(@RequestParam("file") MultipartFile file,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);

        log.info("🎵 CSV 일괄 등록 요청: 파일명={}, 크기={}bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // 파일 유효성 검사
            if (file.isEmpty()) {
                model.addAttribute("error", "업로드된 파일이 비어있습니다.");
                return "song/bulk-register-result";
            }

            if (!isValidCsvFile(file)) {
                model.addAttribute("error", "CSV 파일만 업로드 가능합니다.");
                return "song/bulk-register-result";
            }

            // 일괄 등록 실행
            SongBulkRegisterService.BulkRegisterResult result = songBulkRegisterService.bulkRegisterFromCsv(file);

            // 결과를 DTO로 변환
            SongBulkRegisterResponseDto responseDto = SongBulkRegisterResponseDto.from(result);

            // 모델에 결과 추가
            model.addAttribute("bulkRegisterResult", responseDto);
            model.addAttribute("success", true);

            log.info("✅ CSV 일괄 등록 완료: 성공={}개, 중복={}개, 실패={}개",
                    result.getSuccessCount(), result.getDuplicateCount(), result.getFailureCount());

            return "song/bulk-register-result";

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

            model.addAttribute("error", errorMessage);
            return "song/bulk-register-result";
        }
    }

    /**
     * CSV 파일 업로드 및 일괄 등록 처리 (AJAX용)
     */
    @PostMapping("/bulk-register-ajax")
    @ResponseBody
    public CommonResponse<?> bulkRegisterAjax(@RequestParam("file") MultipartFile file,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);

        log.info("🎵 CSV 일괄 등록 요청 (AJAX): 파일명={}, 크기={}bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // 파일 유효성 검사
            if (file.isEmpty()) {
                return CommonResponse.fail(ResponseCode.INVALID_INPUT, "업로드된 파일이 비어있습니다.");
            }

            if (!isValidCsvFile(file)) {
                return CommonResponse.fail(ResponseCode.INVALID_INPUT, "CSV 파일만 업로드 가능합니다.");
            }

            // 일괄 등록 실행
            SongBulkRegisterService.BulkRegisterResult result = songBulkRegisterService.bulkRegisterFromCsv(file);

            // 결과를 DTO로 변환
            SongBulkRegisterResponseDto responseDto = SongBulkRegisterResponseDto.from(result);

            log.info("✅ CSV 일괄 등록 완료: 성공={}개, 중복={}개, 실패={}개",
                    result.getSuccessCount(), result.getDuplicateCount(), result.getFailureCount());

            return CommonResponse.success(responseDto);

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