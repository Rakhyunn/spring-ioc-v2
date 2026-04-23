package com.ll.domain.testPost.service;

import com.ll.domain.testPost.repository.TestPostRepository;
import com.ll.framework.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestFacadePostService {
    private final TestPostService testPostService;
    private final TestPostRepository testPostRepository;
}
