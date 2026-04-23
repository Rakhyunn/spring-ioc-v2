package com.ll.domain.testPost.service;

import com.ll.domain.testPost.repository.TestPostRepository;
import com.ll.framework.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestPostService {
    private final TestPostRepository testPostRepository;
}
