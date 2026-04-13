# CRM System

CRM для аренды/проката: клиенты, аренда, оборудование, точки, расходы, документы, статистика. **Spring Boot 4**, **Thymeleaf**, **PostgreSQL**, **Spring Security**. Интерфейс на русском, под контекст **РБ**.

Репозиторий: [github.com/vadim-minchik/crm-system](https://github.com/vadim-minchik/crm-system)

## Стек

Java 21 · Spring Boot · JPA/Hibernate · PostgreSQL · Thymeleaf · Apache POI · Maven (`mvnw`)

## Запуск в Docker

Нужен [Docker Desktop](https://www.docker.com/products/docker-desktop/) (движок **Engine running**). В корне проекта:

```powershell
docker compose up --build
```

Сайт: **http://localhost:8080** · пароль БД в контейнере: `docker-compose.yml` (`POSTGRES_PASSWORD` = `DB_PASSWORD`).

## Запуск локально (JDK + PostgreSQL)

1. **JDK 21**, **PostgreSQL**, база `crm_system`.
2. `application.properties` или `application-local.properties` (шаблон: `application-local.properties.example`).
3. `.\mvnw.cmd spring-boot:run` / `./mvnw spring-boot:run` → **http://localhost:8080**

## Тесты и сборка

```powershell
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

На GitHub: **Actions** — CI (`./mvnw test`) для веток `main` и `master`.

## Прочее

- [ER-диаграмма БД](docs/er-diagram.md) (Mermaid)
- Файлы: `app.storage.type=local` (папка `uploads`) или Supabase — см. пример в `application-local.properties.example`
