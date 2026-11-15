package com.example.PantryPal.grpc.generated;

import com.example.PantryPal.grpc.generated.PantryPalProto.*;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Nastavek (Stub) in definicija storitve gRPC po shemi src/main/proto/pantrypal.proto.
 */
public final class PantryPalGrpcServiceGrpc {
    public static final String SERVICE_NAME = "pantrypal.PantryPalGrpcService";

    private PantryPalGrpcServiceGrpc() {}

    @FunctionalInterface
    private interface Parser<T> {
        T parse(InputStream stream) throws IOException;
    }

    private static <T extends ProtoMessage> MethodDescriptor.Marshaller<T> marshaller(Parser<T> parser) {
        return new MethodDescriptor.Marshaller<>() {
            @Override
            public InputStream stream(T value) {
                return new ByteArrayInputStream(value.toByteArray());
            }

            @Override
            public T parse(InputStream stream) {
                try {
                    return parser.parse(stream);
                } catch (IOException e) {
                    throw Status.INTERNAL
                            .withDescription("Napaka pri razčlenjevanju Protocol Buffers sporočila")
                            .withCause(e)
                            .asRuntimeException();
                }
            }
        };
    }

    public static final MethodDescriptor<KategorijaZahtevek, SeznamZalogeOdgovor> METHOD_PRIDOBI_ZALOGO_PO_KATEGORIJI =
            MethodDescriptor.<KategorijaZahtevek, SeznamZalogeOdgovor>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "PridobiZalogoPoKategoriji"))
                    .setSampledToLocalTracing(true)
                    .setRequestMarshaller(marshaller(KategorijaZahtevek::parseFrom))
                    .setResponseMarshaller(marshaller(SeznamZalogeOdgovor::parseFrom))
                    .build();

    public static final MethodDescriptor<StatistikaZahtevek, StatistikaOdgovor> METHOD_IZRACUNAJ_STATISTIKO_ZALOGE =
            MethodDescriptor.<StatistikaZahtevek, StatistikaOdgovor>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "IzracunajStatistikoZaloge"))
                    .setSampledToLocalTracing(true)
                    .setRequestMarshaller(marshaller(StatistikaZahtevek::parseFrom))
                    .setResponseMarshaller(marshaller(StatistikaOdgovor::parseFrom))
                    .build();

    public static final MethodDescriptor<PosodobiKolicinoZahtevek, PostavkaZalogeMsg> METHOD_POSODOBI_KOLICINO =
            MethodDescriptor.<PosodobiKolicinoZahtevek, PostavkaZalogeMsg>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "PosodobiKolicino"))
                    .setSampledToLocalTracing(true)
                    .setRequestMarshaller(marshaller(PosodobiKolicinoZahtevek::parseFrom))
                    .setResponseMarshaller(marshaller(PostavkaZalogeMsg::parseFrom))
                    .build();

    public static final MethodDescriptor<StreamZahtevek, KriticnaPostavkaDogodek> METHOD_SPREMLJAJ_KRITICNO_ZALOGO_STREAM =
            MethodDescriptor.<StreamZahtevek, KriticnaPostavkaDogodek>newBuilder()
                    .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "SpremljajKriticnoZalogoStream"))
                    .setSampledToLocalTracing(true)
                    .setRequestMarshaller(marshaller(StreamZahtevek::parseFrom))
                    .setResponseMarshaller(marshaller(KriticnaPostavkaDogodek::parseFrom))
                    .build();

    public static PantryPalGrpcServiceBlockingStub newBlockingStub(Channel channel) {
        return PantryPalGrpcServiceBlockingStub.newStub(PantryPalGrpcServiceBlockingStub::new, channel);
    }

    public static PantryPalGrpcServiceStub newStub(Channel channel) {
        return PantryPalGrpcServiceStub.newStub(PantryPalGrpcServiceStub::new, channel);
    }

    /**
     * Osnovni strežniški razred za implementacijo 4 RPC klicev.
     */
    public static abstract class PantryPalGrpcServiceImplBase implements BindableService {

        public void pridobiZalogoPoKategoriji(
                KategorijaZahtevek request,
                StreamObserver<SeznamZalogeOdgovor> responseObserver
        ) {
            ServerCalls.asyncUnimplementedUnaryCall(METHOD_PRIDOBI_ZALOGO_PO_KATEGORIJI, responseObserver);
        }

        public void izracunajStatistikoZaloge(
                StatistikaZahtevek request,
                StreamObserver<StatistikaOdgovor> responseObserver
        ) {
            ServerCalls.asyncUnimplementedUnaryCall(METHOD_IZRACUNAJ_STATISTIKO_ZALOGE, responseObserver);
        }

        public void posodobiKolicino(
                PosodobiKolicinoZahtevek request,
                StreamObserver<PostavkaZalogeMsg> responseObserver
        ) {
            ServerCalls.asyncUnimplementedUnaryCall(METHOD_POSODOBI_KOLICINO, responseObserver);
        }

        public void spremljajKriticnoZalogoStream(
                StreamZahtevek request,
                StreamObserver<KriticnaPostavkaDogodek> responseObserver
        ) {
            ServerCalls.asyncUnimplementedUnaryCall(METHOD_SPREMLJAJ_KRITICNO_ZALOGO_STREAM, responseObserver);
        }

        @Override
        public final ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder(SERVICE_NAME)
                    .addMethod(
                            METHOD_PRIDOBI_ZALOGO_PO_KATEGORIJI,
                            ServerCalls.asyncUnaryCall(this::pridobiZalogoPoKategoriji)
                    )
                    .addMethod(
                            METHOD_IZRACUNAJ_STATISTIKO_ZALOGE,
                            ServerCalls.asyncUnaryCall(this::izracunajStatistikoZaloge)
                    )
                    .addMethod(
                            METHOD_POSODOBI_KOLICINO,
                            ServerCalls.asyncUnaryCall(this::posodobiKolicino)
                    )
                    .addMethod(
                            METHOD_SPREMLJAJ_KRITICNO_ZALOGO_STREAM,
                            ServerCalls.asyncServerStreamingCall(this::spremljajKriticnoZalogoStream)
                    )
                    .build();
        }
    }

    /**
     * Sinhroni (Blocking) odjemalčev nastavek (Stub).
     */
    public static final class PantryPalGrpcServiceBlockingStub
            extends AbstractBlockingStub<PantryPalGrpcServiceBlockingStub> {

        private PantryPalGrpcServiceBlockingStub(Channel channel, CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected PantryPalGrpcServiceBlockingStub build(Channel channel, CallOptions callOptions) {
            return new PantryPalGrpcServiceBlockingStub(channel, callOptions);
        }

        public SeznamZalogeOdgovor pridobiZalogoPoKategoriji(KategorijaZahtevek request) {
            return ClientCalls.blockingUnaryCall(
                    getChannel(), METHOD_PRIDOBI_ZALOGO_PO_KATEGORIJI, getCallOptions(), request);
        }

        public StatistikaOdgovor izracunajStatistikoZaloge(StatistikaZahtevek request) {
            return ClientCalls.blockingUnaryCall(
                    getChannel(), METHOD_IZRACUNAJ_STATISTIKO_ZALOGE, getCallOptions(), request);
        }

        public PostavkaZalogeMsg posodobiKolicino(PosodobiKolicinoZahtevek request) {
            return ClientCalls.blockingUnaryCall(
                    getChannel(), METHOD_POSODOBI_KOLICINO, getCallOptions(), request);
        }

        public Iterator<KriticnaPostavkaDogodek> spremljajKriticnoZalogoStream(StreamZahtevek request) {
            return ClientCalls.blockingServerStreamingCall(
                    getChannel(), METHOD_SPREMLJAJ_KRITICNO_ZALOGO_STREAM, getCallOptions(), request);
        }
    }

    /**
     * Asinhroni odjemalčev nastavek (Stub) za neblokirajoče pretakanje v realnem času.
     */
    public static final class PantryPalGrpcServiceStub
            extends AbstractAsyncStub<PantryPalGrpcServiceStub> {

        private PantryPalGrpcServiceStub(Channel channel, CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected PantryPalGrpcServiceStub build(Channel channel, CallOptions callOptions) {
            return new PantryPalGrpcServiceStub(channel, callOptions);
        }

        public void spremljajKriticnoZalogoStream(
                StreamZahtevek request,
                StreamObserver<KriticnaPostavkaDogodek> responseObserver
        ) {
            ClientCalls.asyncServerStreamingCall(
                    getChannel().newCall(METHOD_SPREMLJAJ_KRITICNO_ZALOGO_STREAM, getCallOptions()),
                    request,
                    responseObserver
            );
        }
    }
}
