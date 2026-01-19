package kr.co.postgresql.core;

import org.springframework.stereotype.Service;

@Service
public class HelloCore {
    public String sayHello() {
        return "Hello from Core Module!";
    }
}
