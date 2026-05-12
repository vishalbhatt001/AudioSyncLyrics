# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Whisper language defaults to Hindi: `hi`.
- Optional `lyricsHint` provides expected lyric context.
- Optional `lyricsLanguage` controls display output: `hinglish`, `hindi`, or `english`.
- A strict OpenAI text pass normalizes Whisper tokens for display while preserving timestamp slots.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Lyrics render as karaoke-style neon lines with current word highlighted.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
WHISPER_LANGUAGE=hi
LYRICS_CORRECTION_MODEL=gpt-4o-mini
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
lyricsHint=Transcribe Hindi song lyrics in Hinglish/Roman Hindi. Do not use Devanagari.
lyricsLanguage=hinglish
```

`lyricsLanguage` values:

```text
hinglish  -> Roman Hindi display
hindi     -> Devanagari Hindi display
english   -> conservative English display
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs --lyrics-language=hinglish --lyrics-hint='Transcribe Hindi song lyrics in Hinglish/Roman Hindi. Do not use Devanagari.'"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Whisper language defaults to Hindi: `hi`.
- Optional `lyricsHint` can be pasted in Swagger to improve Hindi spelling.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Uploaded image is animated with slow pan/zoom.
- A cinematic vignette and warm glow overlay is applied.
- A short intro hook appears in the first 2.4 seconds.
- Lyrics render as karaoke-style neon lines: full phrase visible, current sung word highlighted yellow with magenta glow.
- Console logs print method-level tracking and progress percentages.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
WHISPER_LANGUAGE=hi
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
lyricsHint=<optional expected Hindi lyrics>
```

Use `lyricsHint` when Hindi spelling is bad. Paste the expected lyric lines for the 59-second hook.

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs --lyrics-hint='expected Hindi lyrics here'"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Whisper language defaults to Hindi: `hi`.
- Optional `lyricsHint` can be pasted in Swagger to improve Hindi spelling.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Uploaded image is animated with slow pan/zoom.
- A cinematic vignette and warm glow overlay is applied.
- A short intro hook appears in the first 2.4 seconds.
- Lyrics render as karaoke-style lines. The full phrase stays visible and the current sung word is highlighted yellow.
- Console logs print method-level tracking and progress percentages.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
WHISPER_LANGUAGE=hi
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
lyricsHint=<optional expected Hindi lyrics>
```

Use `lyricsHint` when Hindi spelling is bad. Paste the expected lyric lines for the 59-second hook. Whisper still supplies timestamps, but the prompt gives it Hindi spelling context.

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs --lyrics-hint='expected Hindi lyrics here'"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Uploaded image is animated with slow pan/zoom.
- A cinematic vignette and warm glow overlay is applied.
- A short intro hook appears in the first 2.4 seconds.
- Lyrics render as karaoke-style lines. The full phrase stays visible and the current sung word is highlighted yellow.
- Console logs print method-level tracking and progress percentages.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Uploaded image is animated with slow pan/zoom.
- A cinematic vignette and warm glow overlay is applied.
- A short intro hook appears in the first 2.4 seconds.
- Lyric overlays are generated as tight transparent PNGs, then composited by FFmpeg.
- Console logs print method-level tracking and progress percentages.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Uploaded image is animated with a slow pan/zoom.
- Lyric overlays are generated as tight transparent PNGs, then composited by FFmpeg.
- Console logs print method-level tracking and progress percentages.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Lyric overlays are generated as tight transparent PNGs, then composited by FFmpeg.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
progressOverlayPath=generated animated progress bar overlay video
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Audio is compressed locally before Whisper to stay below OpenAI's request size limit.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Synced lyric text is burned at upper-middle screen, `y=35%`.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
overlayDirPath=generated transparent lyric PNG overlays
whisperInputPath=compressed audio sent to Whisper
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Synced lyric text is burned at upper-middle screen, `y=35%`.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

## Run

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:9099/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:9099/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
subtitlesPath=generated .ass subtitle file
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Synced lyric text is burned at upper-middle screen, `y=35%`.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Requirements

```bash
ffmpeg -version
```

Install if missing:

```bash
brew install ffmpeg
```

## Config

Set `.env`:

```bash
OPENAI_API_KEY=sk-...
UPLOAD_DIR=uploads
```

No `PUBLIC_BASE_URL`. The MP4 is local. Swagger returns a local URL like:

```text
http://localhost:8080/assets/{jobId}/shorts-output.mp4
```

## API

Start:

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
```

Response includes:

```text
outputUrl=http://localhost:8080/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
subtitlesPath=generated .ass subtitle file
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- FFmpeg renders locally. No Creatomate bill.
- Output MP4 is hardcoded to `1080x1920`.
- Synced lyric text is burned at upper-middle screen, `y=35%`.
- CTA text is injected at `54s` for `5s`, `y=60%`.

## Requirements

```bash
ffmpeg -version
```

Install if missing:

```bash
brew install ffmpeg
```

## Config

Set keys in `.env`:

```bash
OPENAI_API_KEY=sk-...
PUBLIC_BASE_URL=
UPLOAD_DIR=uploads
```

`PUBLIC_BASE_URL` is optional now. Leave it blank for local Swagger testing. Set it only if you want response URLs to use a tunnel/domain instead of `localhost`.

## API

Start:

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
publicBaseUrl=<optional>
```

Response includes:

```text
outputUrl=/assets/{jobId}/shorts-output.mp4
outputPath=local filesystem path
subtitlesPath=generated .ass subtitle file
```

## CLI

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --output-dir=outputs"
```
# MusicLoad Shorts Funnel

Zero-UI Spring Boot runner for a 59-second YouTube Shorts lyrical render.

## Contract

- Input audio is already trimmed to a 59-second MP3 hook.
- Whisper request uses `response_format=verbose_json`.
- Whisper request uses `timestamp_granularities[]=word`.
- Creatomate root source uses `width=1080` and `height=1920`.
- Synced lyric text is locked at `y=35%`.
- CTA text is injected at `time=54`, `duration=5`, `y=60%`.

## Run

Set keys in `.env`:

```bash
OPENAI_API_KEY=sk-...
CREATOMATE_API_KEY=...
PUBLIC_BASE_URL=https://your-public-domain-or-ngrok-url
UPLOAD_DIR=uploads
```

Start the API:

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Upload endpoint:

```text
POST /api/shorts/renders
Content-Type: multipart/form-data

mp3=<59-second-hook.mp3>
image=<background.jpg>
publicBaseUrl=https://your-public-domain-or-ngrok-url
```

`PUBLIC_BASE_URL` must be a public URL pointing to this Spring Boot app. Localhost will not work for Creatomate unless it is tunneled with something like ngrok.

CLI still works:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--mp3=/absolute/path/hook-59s.mp3 --image=/absolute/path/background.jpg --asset-base-url=https://cdn.example.com/musicload-assets"
```

`--mp3` and `--image` are local files. The same files must also exist under `--asset-base-url` with the same file names, because Creatomate cannot fetch `/Users/...` from your machine.

Generated payload asset URLs:

```text
https://cdn.example.com/musicload-assets/hook-59s.mp3
https://cdn.example.com/musicload-assets/background.jpg
```

The runner logs `CREATOMATE_RENDER_PAYLOAD` before posting to Creatomate.
