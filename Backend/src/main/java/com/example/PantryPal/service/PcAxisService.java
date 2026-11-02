package com.example.PantryPal.service;

import com.example.PantryPal.odprtipodatki.PxParser;
import org.springframework.stereotype.Service;
import java.io.IOException;

/** Pretvarjanje izvornih bajtov PC-Axis v preverjeno statistično tabelo. */
@Service
public class PcAxisService {
    private final PxParser parser;
    public PcAxisService(PxParser parser) { this.parser = parser; }
    public PxParser.Tabela preberi(byte[] vsebina) throws IOException {
        return parser.preberi(vsebina);
    }
}
