package nl.altindag.client;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import com.github.mizosoft.methanol.Methanol;
import com.google.api.client.http.HttpTransport;
import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import feign.Feign;
import kong.unirest.Unirest;
import nl.altindag.sslcontext.SSLFactory;
import okhttp3.OkHttpClient;
import org.apache.http.client.HttpClient;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import retrofit2.Retrofit;

import javax.net.ssl.SSLException;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.URISyntaxException;

import static nl.altindag.client.util.SSLFactoryTestHelper.createSSLFactory;
import static nl.altindag.client.util.AssertJCustomConditions.GSON_CONVERTER_FACTORY;
import static nl.altindag.client.util.AssertJCustomConditions.SUBSTRING_OF_HTTP_OR_HTTPS_SERVER_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(MockitoJUnitRunner.class)
public class ClientConfigShould {

    private final ClientConfig victim = new ClientConfig();

    @Test
    public void createApacheHttpClientWithoutSecurity() {
        HttpClient httpClient = victim.apacheHttpClient(null);

        assertThat(httpClient).isNotNull();
    }

    @Test
    public void createApacheHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        HttpClient httpClient = victim.apacheHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
    }

    @Test
    public void createJdkHttpClientWithoutSecurity() {
        java.net.http.HttpClient httpClient = victim.jdkHttpClient(null);

        assertThat(httpClient).isNotNull();
    }

    @Test
    public void createJdkHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        java.net.http.HttpClient httpClient = victim.jdkHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
    }

    @Test
    public void createRestTemplate() {
        HttpClient httpClient = mock(HttpClient.class);

        RestTemplate restTemplate = victim.restTemplate(httpClient);

        assertThat(restTemplate).isNotNull();
    }

    @Test
    public void createOkHttpClientWithoutSecurity() {
        OkHttpClient okHttpClient = victim.okHttpClient(null);

        assertThat(okHttpClient).isNotNull();
    }

    @Test
    public void createOkHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        OkHttpClient okHttpClient = victim.okHttpClient(sslFactory);

        assertThat(okHttpClient).isNotNull();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getTrustManager();
        verify(sslFactory, times(1)).getHostnameVerifier();

        assertThat(okHttpClient.hostnameVerifier()).isEqualTo(sslFactory.getHostnameVerifier());
        assertThat(okHttpClient.x509TrustManager()).isEqualTo(sslFactory.getTrustManager());
    }

    @Test
    public void createNettyHttpClientWithoutSecurity() throws SSLException {
        reactor.netty.http.client.HttpClient httpClient = victim.nettyHttpClient(null);

        assertThat(httpClient).isNotNull();
    }

    @Test
    public void createNettyHttpClientWithOneWayAuthentication() throws SSLException {
        SSLFactory sslFactory = createSSLFactory(true, false);

        reactor.netty.http.client.HttpClient httpClient = victim.nettyHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
        verify(sslFactory, times(1)).getTrustManager();
    }

    @Test
    public void createNettyHttpClientWithTwoWayAuthentication() throws SSLException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        reactor.netty.http.client.HttpClient httpClient = victim.nettyHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
        verify(sslFactory, times(1)).getKeyManager();
        verify(sslFactory, times(1)).getTrustManager();
    }

    @Test
    public void createWebClientWithNetty() {
        reactor.netty.http.client.HttpClient httpClient = mock(reactor.netty.http.client.HttpClient.class);
        WebClient webClient = victim.webClientWithNetty(httpClient);

        assertThat(webClient).isNotNull();
    }

    @Test
    public void createJettyHttpClientWithoutSecurity() {
        org.eclipse.jetty.client.HttpClient httpClient = victim.jettyHttpClient(null);

        assertThat(httpClient).isNotNull();
    }

    @Test
    public void createJettyHttpClientWithOneWayAuthentication() {
        SSLFactory sslFactory = createSSLFactory(true, false);

        org.eclipse.jetty.client.HttpClient httpClient = victim.jettyHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(3)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
        verify(sslFactory, times(0)).getTrustStores();
        verify(sslFactory, times(0)).getIdentities();
    }

    @Test
    public void createJettyHttpClientWithTwoWayAuthentication() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        org.eclipse.jetty.client.HttpClient httpClient = victim.jettyHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(3)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
        verify(sslFactory, times(0)).getTrustStores();
        verify(sslFactory, times(0)).getIdentities();
    }

    @Test
    public void createWebClientWithJetty() {
        org.eclipse.jetty.client.HttpClient httpClient = mock(org.eclipse.jetty.client.HttpClient.class);
        WebClient webClient = victim.webClientWithJetty(httpClient);

        assertThat(webClient).isNotNull();
    }

    @Test
    public void createJerseyClientWithoutSecurity() {
        Client client = victim.jerseyClient(null);

        assertThat(client).isNotNull();

        client.close();
    }

    @Test
    public void createJerseyClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        Client client = victim.jerseyClient(sslFactory);

        assertThat(client).isNotNull();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        client.close();
    }

    @Test
    public void createOldJerseyClientWithoutSecurity() {
        com.sun.jersey.api.client.Client client = victim.oldJerseyClient(null);

        assertThat(client).isNotNull();

        client.destroy();
    }

    @Test
    public void createOldJerseyClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        com.sun.jersey.api.client.Client client = victim.oldJerseyClient(sslFactory);

        assertThat(client).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        client.destroy();
    }

    @Test
    public void createGoogleHttpClientWithoutSecurity() throws IOException {
        HttpTransport httpTransport = victim.googleHttpClient(null);

        assertThat(httpTransport).isNotNull();

        httpTransport.shutdown();
    }

    @Test
    public void createGoogleHttpClientWithSecurity() throws IOException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        HttpTransport httpTransport = victim.googleHttpClient(sslFactory);

        assertThat(httpTransport).isNotNull();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        httpTransport.shutdown();
    }

    @Test
    public void createUnirestWithoutSecurity() {
        victim.unirest(null);

        assertThat(Unirest.primaryInstance().config().getSslContext()).isNull();

        Unirest.shutDown();
    }

    @Test
    public void createUnirestWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        victim.unirest(sslFactory);

        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();

        assertThat(Unirest.primaryInstance().config().getSslContext()).isEqualTo(sslFactory.getSslContext());

        Unirest.shutDown();
    }

    @Test
    public void createRetrofitWithProvidedOkHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Retrofit retrofit = victim.retrofit(okHttpClient);

        assertThat(retrofit).isNotNull();
        assertThat(retrofit.baseUrl().toString()).has(SUBSTRING_OF_HTTP_OR_HTTPS_SERVER_URL);
        assertThat(retrofit.converterFactories()).has(GSON_CONVERTER_FACTORY);
    }

    @Test
    public void createFinagleClientWithoutSecurity() throws URISyntaxException {
        Service<Request, Response> service = victim.finagle(null);

        assertThat(service.isAvailable()).isTrue();
        assertThat(service.status().toString()).isEqualTo("Open");

        service.close();
    }

    @Test
    public void createFinagleClientWithSecurity() throws URISyntaxException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        Service<Request, Response> service = victim.finagle(sslFactory);

        verify(sslFactory, times(1)).getSslContext();

        assertThat(service.isAvailable()).isTrue();
        assertThat(service.status().toString()).isEqualTo("Open");

        service.close();
    }

    @Test
    public void createAkkaHttpClientWithoutSecurity() {
        Http http = victim.akkaHttpClient(null, ActorSystem.create());

        assertThat(http).isNotNull();
    }

    @Test
    public void createAkkaHttpClientWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(false, true);

        Http http = victim.akkaHttpClient(sslFactory, ActorSystem.create());

        assertThat(http).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
    }

    @Test
    public void createActorSystem() {
        ActorSystem actorSystem = victim.actorSystem();

        assertThat(actorSystem).isNotNull();
        assertThat(actorSystem.name()).isEqualTo("ClientConfig");
    }

    @Test
    public void createAsyncHttpClientWithoutSecurity() throws SSLException {
        AsyncHttpClient httpClient = victim.asyncHttpClient(null);

        assertThat(httpClient).isNotNull();
        assertThat(httpClient.getConfig().getSslContext()).isNull();
    }

    @Test
    public void createAsyncHttpClientWithOneWayAuthentication() throws SSLException {
        SSLFactory sslFactory = createSSLFactory(true, false);

        AsyncHttpClient httpClient = victim.asyncHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        assertThat(httpClient.getConfig().getSslContext()).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
        verify(sslFactory, times(1)).getTrustManager();
    }

    @Test
    public void createAsyncHttpClientWithTwoWayAuthentication() throws SSLException {
        SSLFactory sslFactory = createSSLFactory(false, true);

        AsyncHttpClient httpClient = victim.asyncHttpClient(sslFactory);

        assertThat(httpClient).isNotNull();
        assertThat(httpClient.getConfig().getSslContext()).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
        verify(sslFactory, times(1)).getKeyManager();
        verify(sslFactory, times(1)).getTrustManager();
    }

    @Test
    public void createFeignWithoutSecurity() throws SSLException {
        Feign.Builder feignBuilder = victim.feign(null);

        assertThat(feignBuilder).isNotNull();
    }

    @Test
    public void createFeignWithSecurity() throws SSLException {
        SSLFactory sslFactory = createSSLFactory(true, true);

        Feign.Builder feignBuilder = victim.feign(sslFactory);

        assertThat(feignBuilder).isNotNull();
        verify(sslFactory, times(1)).getSslContext();
        verify(sslFactory, times(1)).getHostnameVerifier();
    }

    @Test
    public void createMethanolWithoutSecurity() {
        Methanol httpClient = victim.methanol(null);

        assertThat(httpClient).isNotNull();
    }

    @Test
    public void createMethanolWithSecurity() {
        SSLFactory sslFactory = createSSLFactory(true, true);

        Methanol httpClient = victim.methanol(sslFactory);

        assertThat(httpClient).isNotNull();
        verify(sslFactory, times(2)).getSslContext();
    }

}
