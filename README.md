# 侘寂 · Slow Gallery

A containerized full-stack photo gallery: upload an image with a few words, and
it joins a handmade **bento** grid styled in a muted **wabi-sabi** aesthetic.
Images live in **S3**, descriptions in **PostgreSQL**, and reads are served
through **CloudFront**. Built to run as a single container on **ECS Fargate**.

> This repository is the **application** only. The infrastructure (VPC, RDS,
> S3, CloudFront, ECS, CodeDeploy blue/green, the CodePipeline + OIDC roles)
> lives in a separate repo: `aws-photo-uploader-iac`. The two are intentionally
> decoupled — pushing an image here is what triggers a deployment there.

---

## Stack

| Concern        | Choice                                               |
| -------------- | ---------------------------------------------------- |
| Language / fw  | Java 21 · Spring Boot 3.4 (MVC + Thymeleaf + JPA)    |
| Image storage  | Amazon S3 (private) via AWS SDK v2, read via CloudFront |
| Metadata       | Amazon RDS PostgreSQL                                |
| View layer     | Server-rendered Thymeleaf, bento masonry + lightbox  |
| Container      | Multi-stage Docker → `eclipse-temurin:21-jre`, binds `:80` |

## How images flow

The production S3 bucket is **fully private** — all public access blocked, no
ACLs, and the bucket policy trusts **only** the CloudFront Origin Access Control
(OAC). CloudFront is GET/HEAD only. So:

- **Upload:** browser → app (multipart) → `S3:PutObject` using the ECS **task
  role**. The app stores the object key + description in PostgreSQL.
- **Display:** the app renders `https://<CLOUDFRONT_DOMAIN>/<object-key>`; the
  browser fetches the image from CloudFront, which signs the origin request to
  the private bucket.

```
 upload   browser ──multipart──▶ ECS task ──PutObject──▶  S3 (private)
                                    │                        ▲
                                    └──INSERT key+desc──▶ RDS │ OAC (SigV4)
 display  browser ──────────GET────────────▶ CloudFront ─────┘
```

Locally there is no S3/CloudFront: a `local` storage driver writes to disk and
the app serves the bytes at `/media/**`, so the exact same code path runs
offline.

## Configuration (the ECS contract)

The app reads these environment variables — the same ones the infrastructure
repo's task definition injects:

| Variable           | Set by         | Purpose                                              |
| ------------------ | -------------- | ---------------------------------------------------- |
| `DB_HOST`          | task def       | RDS endpoint                                         |
| `DB_PORT`          | task def       | `5432`                                               |
| `DB_CREDENTIALS`   | Secrets Manager| JSON secret → `username` / `password` / `dbname`     |
| `IMAGE_BUCKET`     | task def       | S3 bucket name for uploads                           |
| `CLOUDFRONT_DOMAIN`| task def       | CloudFront domain used to build image URLs           |
| `AWS_REGION`       | runtime        | S3 client region (falls back to `eu-north-1`)        |
| `SERVER_PORT`      | task def / img | Listen port (defaults to `80`)                       |

**Storage driver** is auto-selected: `STORAGE_DRIVER` (`local`|`s3`) is honoured
if set; otherwise the presence of `IMAGE_BUCKET` chooses `s3`, else `local`.
**DB** is assembled from `DB_CREDENTIALS` (prod) or plain `DB_NAME` /
`DB_USERNAME` / `DB_PASSWORD` (local) — explicit vars always win.

`GET /` doubles as the ALB blue/green health check (returns 200).

## Run it locally (offline)

No AWS needed — docker-compose brings up Postgres and the app with local-disk
storage:

```bash
make dev          # docker compose up --build + tail logs
# open http://localhost:8080
```

```bash
make down         # stop
make clean        # stop + wipe the postgres/uploads volumes
make test         # mvn verify (unit tests, no DB/AWS)
```

Running outside Docker? Port 80 is privileged, so set `SERVER_PORT=8080` and
point the `DB_*` vars at a local Postgres.

## Deploy

CI (`.github/workflows/deploy.yml`) runs on push to `main`: it tests, assumes an
AWS role via **GitHub OIDC** (no static keys), builds the image, and pushes
`:latest` to ECR. The infrastructure repo's CodePipeline watches that ECR push
and runs the **CodeDeploy blue/green** rollout onto ECS Fargate.

Required GitHub repo settings:

- Secret `AWS_GHA_ROLE_ARN` — the OIDC role to assume
- Variables `AWS_REGION`, `ECR_REPOSITORY`

## Layout

```
src/main/java/com/example/photogallery/
  config/    DbCredentials, DataSourceConfig, StorageConfig, WebConfig
  storage/   PhotoStorage + LocalDiskStorage / S3Storage
  photo/     Photo entity, repository, PhotoService, PhotoView
  web/       GalleryController, UploadExceptionHandler
src/main/resources/
  templates/ gallery.html, upload.html
  static/    css/wabi-sabi.css, js/gallery.js
Dockerfile · docker-compose.yml · Makefile
```
