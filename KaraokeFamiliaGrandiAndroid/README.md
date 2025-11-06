# 🎤 Karaoke Família Grandi (Android)

App Android simples para karaokê que:
- Lista vídeos públicos de uma pasta do Google Drive
- Toca os vídeos via ExoPlayer
- Possui fila de espera e busca
- Faz uma pontuação **simplificada** baseada no microfone (detecção de pitch com TarsosDSP)
- Gera APK automaticamente via GitHub Actions

## Pasta do Drive
Por padrão usa a pasta: `1iq4OrWXNRIFw6VG3MbPkdSmIZNLSAl9M` (do seu link).  
Você pode alterar em `app/src/main/res/values/strings.xml` o valor de `folder_id`.

## Pré-requisitos
1. Criar um **API Key** no Google Cloud e **habilitar a Google Drive API** para este projeto.
2. Tornar **públicos** os vídeos da pasta ou, ao menos, acessíveis via link.
3. No código, a chave é lida de `BuildConfig.GOOGLE_DRIVE_API_KEY`:
   - Localmente: edite no `app/build.gradle.kts` substituindo `YOUR_API_KEY`.
   - No GitHub Actions: salve a chave como **Secret** `GDRIVE_API_KEY`.

## Rodar localmente
- Abrir no Android Studio (Giraffe+), sincronizar Gradle e executar em um dispositivo ou emulador.
- Permitir acesso ao **microfone** quando solicitado.

## CI/CD (GitHub Actions)
Este repositório traz um workflow que:
- Instala o JDK 21 e Gradle 8.x
- Executa `gradle assembleRelease`
- Publica `app-release.apk` como **artefato baixável** do workflow

> Dica: crie um repositório no GitHub chamado `KARAOKEFAMILIAGRANDI`, suba os arquivos, e adicione o Secret `GDRIVE_API_KEY` em Settings → Secrets → Actions.

## Limitações
- A pontuação é **básica** (não compara com melodia/lyrics). Para algo avançado, podemos integrar display de letras + modelo de pitch/time-scoring.
- O streaming do Drive usa URL pública via API; se algum arquivo não estiver público, o player pode falhar.

## Estrutura
```
KaraokeFamiliaGrandiAndroid/
  app/
    src/main/java/com/familiagrandi/karaoke/...
    src/main/res/...
    build.gradle.kts
  build.gradle.kts
  settings.gradle.kts
  .github/workflows/android.yml
```

## Licença
MIT