package com.smarttrans.ai.auth.service;

import com.smarttrans.ai.auth.dto.LoginRequest;
import com.smarttrans.ai.auth.dto.LoginResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request);
}
