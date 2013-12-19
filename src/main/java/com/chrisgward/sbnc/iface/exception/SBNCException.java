package com.chrisgward.sbnc.iface.exception;

public class SBNCException extends RuntimeException {
	public SBNCException(String message) {
		super(message);
	}

	public SBNCException(Exception e) {
		super(e);
	}
}
