package com.xiaoniu.aftermarket.common.service.impl;

import com.xiaoniu.aftermarket.common.service.SequenceService;
import org.springframework.stereotype.Service;

@Service
public class SequenceServiceImpl implements SequenceService {

    @Override
    public String next(String seqType) {
        throw new UnsupportedOperationException("TODO: implement sequence generation in a later phase");
    }
}
