# Fontes e Referências - Chord

> **Data da pesquisa**: 2024-10-21  
> **Versão do documento**: 1.0  
> **Critério**: Apenas fontes oficiais, documentação pública, projetos open source.  
> **Convenção de status**: **Visto** = confirmado na fonte oficial; **Inferido** = deduzido da documentação; **Desconhecido** = não confirmado.

---

## 1. Audius

| Item | Detalhes | Status |
|------|----------|--------|
| **Nome** | Audius | Visto |
| **Website** | https://audius.co | Visto |
| **Documentação da API** | https://docs.audius.co/ | Visto |
| **Endpoint Base** | `https://api.audius.co/v1` | Visto |
| **Autenticação** | Opcional (`app_name` query param) | Visto |
| **Rate Limit** | ~60 req/min (não documentado oficialmente) | Inferido |
| **Busca de Faixas** | `GET /v1/tracks/search?q={query}&limit={n}` | Visto |
| **Detalhes da Faixa** | `GET /v1/tracks/{track_id}` | Visto |
| **Stream de Áudio** | `GET /v1/tracks/{track_id}/stream` | Visto |
| **Usuário/Artista** | `GET /v1/users/{user_id}` | Visto |
| **Tracks do Artista** | `GET /v1/users/{user_id}/tracks` | Visto |
| **Trending** | `GET /v1/tracks/trending?time=week` | Visto |
| **Formato de Áudio** | MP3 / AAC (varia) | Inferido |
| **Qualidade** | 128-320 kbps (variável) | Inferido |
| **Licença** | Terms of Service Audius | Visto |
| **Download Permitido** | **Não** (ToS proíbe) | Visto |
| **Atribuição Obrigatória** | "Powered by Audius" + link | Visto |
| **Uso Comercial** | Não permitido | Visto |
| **ToS URL** | https://audius.co/terms | Visto |
| **API Docs URL** | https://docs.audius.co/ | Visto |

**Notas**: Audius é uma plataforma descentralizada. A API pública é estável mas não documenta rate limits oficiais. Recomenda-se `app_name` para identificação.

---

## 2. Jamendo

| Item | Detalhes | Status |
|------|----------|--------|
| **Nome** | Jamendo | Visto |
| **Website** | https://www.jamendo.com | Visto |
| **Developer Portal** | https://developer.jamendo.com/ | Visto |
| **Endpoint Base** | `https://api.jamendo.com/v3.0` | Visto |
| **Autenticação** | `client_id` obrigatório (query param) | Visto |
| **Rate Limit** | 60 req/min (documentado) | Visto |
| **Busca de Faixas** | `GET /tracks/?client_id={id}&search={query}&limit={n}&format=json` | Visto |
| **Detalhes da Faixa** | `GET /tracks/?ids={id}&client_id={id}&format=json&include=musicinfo` | Visto |
| **Artista** | `GET /artists/?id={id}&client_id={id}&format=json` | Visto |
| **Álbum** | `GET /albums/?id={id}&client_id={id}&format=json` | Visto |
| **Tracks do Artista** | `GET /tracks/?artist_id={id}&client_id={id}&limit={n}` | Visto |
| **Tracks do Álbum** | `GET /tracks/?album_id={id}&client_id={id}&format=json` | Visto |
| **Stream/Download** | `audiodownload` (se permitido) ou `audio` (stream) | Visto |
| **Licenças Suportadas** | CC BY, CC BY-SA, CC BY-NC, CC BY-NC-SA, CC BY-ND, CC BY-NC-ND | Visto |
| **Download Permitido** | **Sim** (quando `audiodownload_allowed=true` e licença permite) | Visto |
| **Atribuição Obrigatória** | Conforme licença CC específica | Visto |
| **Uso Comercial** | Depende da licença (CC BY = sim, CC BY-NC = não) | Visto |
| **Developer Portal** | https://developer.jamendo.com/ | Visto |
| **API Docs** | https://developer.jamendo.com/v3.0 | Visto |

**Notas**: Requer registro no Developer Portal para obter `client_id`. Licenças Creative Commons variam por faixa - verificar campo `license` na resposta.

---

## 3. MusicBrainz

| Item | Detalhes | Status |
|------|----------|--------|
| **Nome** | MusicBrainz | Visto |
| **Website** | https://musicbrainz.org | Visto |
| **Documentação da API** | https://musicbrainz.org/doc/Development/XML_Web_Service/Version_2 | Visto |
| **Endpoint Base** | `https://musicbrainz.org/ws/2` | Visto |
| **Autenticação** | Nenhuma (User-Agent obrigatório) | Visto |
| **Rate Limit** | 1 req/seg (50 req/min) | Visto |
| **User-Agent Obrigatório** | `AppName/Version (contato@email.com)` | Visto |
| **Busca de Gravações** | `GET /recording/?query={query}&limit={n}&fmt=json` | Visto |
| **Detalhes da Gravação** | `GET /recording/{mbid}?fmt=json&inc=artists+releases+tags` | Visto |
| **Artista** | `GET /artist/{mbid}?fmt=json&inc=aliases+tags+ratings` | Visto |
| **Release Group (Álbum)** | `GET /release-group/{mbid}?fmt=json&inc=artists+releases+tags` | Visto |
| **Busca por Artista** | `GET /recording/?query=artist:{mbid}&limit={n}` | Visto |
| **Busca por Álbum** | `GET /recording/?query=releases:{mbid}&fmt=json` | Visto |
| **Cobertura de Arte** | `https://coverartarchive.org/release/{mbid}/front-{size}.jpg` | Visto |
| **Licença dos Dados** | MusicBrainz License (similar CC0) | Visto |
| **Download de Áudio** | **Não** (apenas metadados) | Visto |
| **Política de Uso** | https://musicbrainz.org/doc/MusicBrainz_License | Visto |
| **Política de Rate Limit** | https://musicbrainz.org/doc/XML_Web_Service/Rate_Limiting | Visto |

**Notas**: MusicBrainz não hospeda áudio - apenas metadados. Cover Art Archive é serviço separado. User-Agent identificando o app é obrigatório e deve incluir contato.

---

## 4. Cover Art Archive

| Item | Detalhes | Status |
|------|----------|--------|
| **Nome** | Cover Art Archive | Visto |
| **Website** | https://coverartarchive.org/ | Visto |
| **Endpoint Base** | `https://coverartarchive.org` | Visto |
| **Imagem Frontal** | `https://coverartarchive.org/release/{mbid}/front-{size}.jpg` | Visto |
| **Tamanhos Disponíveis** | 250, 300, 500, 1200 | Inferido |
| **Licença** | CC0 / Variada (ver por imagem) | Visto |
| **Documentação** | https://coverartarchive.org/ | Visto |

---

## 5. Provedores NÃO Utilizados (e Porquê)

| Provedor | Motivo | Status |
|----------|--------|--------|
| **YouTube / YouTube Music** | API privada (InnerTube), ToS proíbe scraping, requer PO-token/BotGuard, risco legal alto | Visto |
| **Spotify** | Requer Premium para streaming completo, Web API não fornece streaming de áudio completo gratuito | Visto |
| **SoundCloud** | API restrita, sem tier gratuito para streaming completo | Visto |
| **InnerTube / youtubei** | APIs privadas do Google, risco legal, ToS violation | Visto |
| **Deezer** | API requer parceria, sem tier gratuito para streaming | Inferido |
| **Tidal** | Sem API pública gratuita | Inferido |
| **Apple Music** | Sem API pública, requer MusicKit JS + token de desenvolvedor | Inferido |
| **Bandcamp** | Sem API pública documentada para streaming | Inferido |

---

## 6. Bibliotecas e Dependências (Licenças)

| Biblioteca | Versão | Licença | Uso |
|------------|--------|---------|-----|
| Kotlin stdlib | 2.4.20 | Apache-2.0 | Core |
| Compose Multiplatform | 2025.09.00 | Apache-2.0 | UI |
| Compose Material3 | 2025.09.00 | Apache-2.0 | UI Components |
| Media3 ExoPlayer | 1.6.1 | Apache-2.0 | Android Player |
| Media3 Session | 1.6.1 | Apache-2.0 | Media Session |
| Ktor Client | 3.6.0 | Apache-2.0 | HTTP Client |
| Ktor Serialization | 3.6.0 | Apache-2.0 | JSON |
| Kotlinx Serialization | 1.11.0 | Apache-2.0 | JSON |
| Kotlinx Coroutines | 1.11.0 | Apache-2.0 | Async |
| Kotlinx Datetime | 0.6.1 | Apache-2.0 | Time |
| Room | 2.6.1 | Apache-2.0 | Database |
| Room Compiler (KSP) | 2.6.1 | Apache-2.0 | Code Gen |
| OkHttp | 4.12.0 | Apache-2.0 | HTTP Engine |
| Okio | 3.10.0 | Apache-2.0 | I/O |
| Kotlinx Coroutines Android | 1.11.0 | Apache-2.0 | Android Coroutines |
| Lifecycle Runtime Compose | 2.8.7 | Apache-2.0 | Lifecycle |
| Lifecycle ViewModel Compose | 2.8.7 | Apache-2.0 | ViewModel |
| Navigation Compose | 2.8.7 | Apache-2.0 | Navigation |
| Activity Compose | 1.9.3 | Apache-2.0 | Activity |
| Core KTX | 1.15.0 | Apache-2.0 | Android Extensions |

**Todas as dependências são Apache-2.0** (compatível com GPL-3.0).

---

## 7. Fontes de Referência Adicionais

| Recurso | URL | Status |
|---------|-----|--------|
| Audius API Docs | https://docs.audius.co/ | Visto |
| Jamendo Developer | https://developer.jamendo.com/ | Visto |
| MusicBrainz API | https://musicbrainz.org/doc/Development/XML_Web_Service/Version_2 | Visto |
| Cover Art Archive | https://coverartarchive.org/ | Visto |
| MusicBrainz License | https://musicbrainz.org/doc/MusicBrainz_License | Visto |
| Jamendo API Docs | https://developer.jamendo.com/v3.0 | Visto |
| Audius ToS | https://audius.co/terms | Visto |
| Android Media3 Guide | https://developer.android.com/guide/topics/media/media3 | Visto |
| Compose Multiplatform | https://www.jetbrains.com/lp/compose-multiplatform/ | Visto |
| Kotlin Multiplatform | https://kotlinlang.org/docs/multiplatform.html | Visto |
| GPL-3.0 License | https://www.gnu.org/licenses/gpl-3.0.html | Visto |

---

## 8. Decisões de Arquitetura Baseadas nas Fontes

| Decisão | Baseada Em | Status |
|---------|------------|--------|
| Não usar YouTube/Spotify | ToS + risco legal | Visto |
| Jamendo requer client_id | Developer Portal | Visto |
| MusicBrainz requer User-Agent | Rate Limit Policy | Visto |
| Audius sem download | ToS | Visto |
| Jamendo download condicional | Licença CC por faixa | Visto |
| MusicBrainz apenas metadados | Documentação | Visto |
| GPL-3.0 para Chord | Dependências GPL (InnerTubeX se usado) | Inferido |
| Apache-2.0 para deps | Compatível com GPL-3.0 | Visto |

---

## 9. Atualização da Pesquisa

| Data | Autor | Alterações |
|------|-------|------------|
| 2024-10-21 | gur0s | Criação inicial |
| | | |

---

**Próxima revisão recomendada**: A cada 6 meses ou quando houver mudanças nas APIs/ToS dos provedores.