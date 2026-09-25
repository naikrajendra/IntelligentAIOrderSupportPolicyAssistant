package com.example.ordersupport.support;

import org.springframework.stereotype.Service;

@Service
public class SupportQueryEventService {

    private final SupportQueryEventRepository supportQueryEventRepository;

    public SupportQueryEventService(SupportQueryEventRepository supportQueryEventRepository) {
        this.supportQueryEventRepository = supportQueryEventRepository;
    }

    public void save(SupportQueryEvent event) {
        if (event == null) {
            return;
        }
        supportQueryEventRepository.save(event);
    }
}
