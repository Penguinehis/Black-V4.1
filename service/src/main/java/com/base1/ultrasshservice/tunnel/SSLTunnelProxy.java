package com.base1.ultrasshservice.tunnel;

import com.base1.ultrasshservice.BuildConfig;
import com.trilead.ssh2.ProxyData;
import java.net.Socket;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import com.base1.ultrasshservice.logger.SkStatus;

import org.conscrypt.Conscrypt;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HandshakeCompletedEvent;

import java.security.Security;

public class SSLTunnelProxy implements ProxyData

{
	class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
        private final String val$host;
        private final int val$port;
        private final SSLSocket val$sslSocket;



        HandshakeTunnelCompletedListener( String str, int i, SSLSocket sSLSocket) {
            this.val$host = str;
            this.val$port = i;
            this.val$sslSocket = sSLSocket;
        }

        public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
			//SkStatus.logInfo(new StringBuffer().append("<b>Estabelecida (").append(handshakeCompletedEvent.getSession().getProtocol()).append(") conexão com ").append(val$host).append(":").append(this.val$port).append(" usando ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
			//SkStatus.logInfo(new StringBuffer().append("<b>Protocolo ").append(handshakeCompletedEvent.getSession().getProtocol()).append("").append(" usando ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
		    //SkStatus.logInfo(new StringBuffer().append("Supported cipher suites: ").append(Arrays.toString(this.val$sslSocket.getSupportedCipherSuites())).toString());
			//SkStatus.logInfo(new StringBuffer().append("Enabled cipher suites: ").append(Arrays.toString(this.val$sslSocket.getEnabledCipherSuites())).toString());
           // SkStatus.logInfo(new StringBuffer().append("SSL: Supported protocols: <br>").append(Arrays.toString(val$sslSocket.getSupportedProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			//SkStatus.logInfo(new StringBuffer().append("SSL: Protocolos habilitados: <br>").append(Arrays.toString(val$sslSocket.getEnabledProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			//SkStatus.logInfo("Usando cipher " + handshakeCompletedEvent.getSession().getCipherSuite());
			SkStatus.logInfo("Usando protocolo " + handshakeCompletedEvent.getSession().getProtocol());
			SkStatus.logInfo("Handshake finalizado!");
        }
    }

	private String stunnelServer;
	private int stunnelPort;
	//private int stunnelPort = 443;
	private String stunnelHostSNI;
	private static final int delay = 1000; // in millis



	static {
		try {
			Security.insertProviderAt(Conscrypt.newProvider(), 1);

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}}

	private Socket input;

	private Socket mSocket;

	public SSLTunnelProxy(String server, int port, String hostSni) {
		this.stunnelServer = server;
		this.stunnelPort = port;
		this.stunnelHostSNI = hostSni;
	}

	public void SSLSupport(Socket in)
	{
		input = in;
		//http = ApplicationBase.getUtils();
		//dsp = ApplicationBase.getDefSharedPreferences();
	}



	private void sendForwardSuccess(Socket socket) throws IOException
	{
		String respond = "HTTP/1.1 200 OK\r\n\r\n";
		socket.getOutputStream().write(respond.getBytes());
		socket.getOutputStream().flush();
	}

	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException
	{
		//ver pq nao funciona
		//sendForwardSuccess(input);
		mSocket = SocketChannel.open().socket();
		mSocket.connect(new InetSocketAddress(stunnelServer, stunnelPort));

		if (mSocket.isConnected()) {
			mSocket = doSSLHandshake(hostname, stunnelHostSNI, port);



		}

		return mSocket;
	}


	private SSLSocket doSSLHandshake(String host, String sni, int port) throws IOException {
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers()
					{
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType)
					{
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType)
					{
					}
				}
		};

        try {
			X509TrustManager tm = Conscrypt.getDefaultX509TrustManager();
			SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
			sslContext.init(null, new TrustManager[] { tm }, null);

			TLSSocketFactory tsf = new TLSSocketFactory();
			SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);
			/*SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault()
					.createSocket(host, port);*/

			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);

			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}
			//MOSTRAR SNI EM DEBUG
			if (BuildConfig.DEBUG) {
				//MOSTRA PAYLOAD
				SkStatus.logInfo("Usando SNI: " + sni);
			}else{
				//NÃO MOSTRA PAYLOAD
				SkStatus.logInfo("Configurando SNI..." );
			}
			socket.setEnabledProtocols(socket.getSupportedProtocols());
			//socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());
            socket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket));
            SkStatus.logInfo("Iniciando SSL Handshake...");
			socket.startHandshake();
			return socket;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("Não foi possível concluir SSL handshake: ").append(e).toString());
            throw iOException;
        }
    }

	@Override
	public void close()
	{
		try {
			if (mSocket != null) {
				mSocket.close();
			}
		} catch(IOException e) {}
	}

	private void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
		if (factory instanceof android.net.SSLCertificateSocketFactory && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			((android.net.SSLCertificateSocketFactory)factory).setHostname(socket, hostname);
		} else {
			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}
		}
	}

}
