package kr.co.postgresql.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of("message", "여기까지 오시느라 수많은 에러와 싸우며 삽질(?) 좀 하셨죠? 대단합니다! 👏👏 그 끈기에 박수를 보내며, 당신의 멋진 개발 여정을 응원합니다. https://postgresql.co.kr");
    }
}
