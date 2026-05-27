package com.sprintlog.sprintlogboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


// 관련된 키 묶음을 타입 안전한 객체로 받는 권장 패턴
@ConfigurationProperties(prefix = "sprintlog")
public class SprintLogProperties {

    private String welcomeMessage;

    private SampleData sampleData = new SampleData();

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public SampleData getSampleData() {
        return sampleData;
    }

    public void setSampleData(SampleData sampleData) {
        this.sampleData = sampleData;
    }

    // 중첩된 설정 - 내부 클래스를 하나 선언해서 표현
    public static class SampleData {
        private boolean enabled;
        private int count;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

}
