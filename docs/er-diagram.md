# ER-диаграмма (модель данных CRM System)

Диаграмма построена по JPA-сущностям в `src/main/java/com/studio/crm_system/entity`. Имена таблиц — как в `@Table(name = "...")`.

Связи **«многие ко многим»** реализованы таблицами **`rental_equipment`** и **`booking_equipment`** (отдельных сущностей в коде нет).

```mermaid
erDiagram
    pre_categories ||--o{ categories : "pre_category_id"
    categories ||--o{ categories : "parent_category_id"
    categories ||--o{ tool_names : "category_id"
    tool_names ||--o{ equipment : "tool_name_id"
    points ||--o{ equipment : "point_id"
    points ||--o{ users : "point_id"
    points ||--o{ rentals : "point_id"

    users ||--o{ users : "created_by_id"
    users ||--o{ clients : "added_by_user_id"
    users ||--o{ rentals : "created_by_staff_id"
    users ||--o{ rentals : "handed_over_by_staff_id"
    users ||--o{ expenses : "created_by_id"
    users ||--o{ recurring_expenses : "created_by_id"
    users ||--o{ document_templates : "created_by_id"
    users ||--o{ client_reviews : "author_id"
    users ||--o{ owner_payouts : "recorded_by_user_id"
    users ||--o{ callback_requests : "creator_user_id"

    clients ||--o{ rentals : "client_id"
    clients ||--o{ client_reviews : "client_id"
    clients ||--o{ callback_requests : "client_id"

    equipment ||--o{ equipment_owners : "equipment_id"
    equipment ||--o{ owner_payouts : "equipment_id"
    equipment ||--o{ callback_requests : "equipment_id"
    equipment_owners ||--o{ owner_payouts : "equipment_owner_id"

    rentals }o--o{ equipment : "rental_equipment"
    bookings }o--o{ equipment : "booking_equipment"

    pre_categories {
        bigint id PK
        string name
    }

    categories {
        bigint id PK
        string name
        bigint pre_category_id FK
        bigint parent_category_id FK
    }

    tool_names {
        bigint id PK
        string name
        bigint category_id FK
    }

    points {
        bigint id PK
        string name
    }

    users {
        bigint id PK
        string login
        string role
        bigint point_id FK
        bigint created_by_id FK
    }

    clients {
        bigint id PK
        bigint added_by_user_id FK
    }

    equipment {
        bigint id PK
        bigint tool_name_id FK
        bigint point_id FK
    }

    equipment_owners {
        bigint id PK
        bigint equipment_id FK
    }

    rentals {
        bigint id PK
        bigint client_id FK
        bigint point_id FK
        bigint created_by_staff_id FK
        bigint handed_over_by_staff_id FK
    }

    bookings {
        bigint id PK
        string phone_number
    }

    owner_payouts {
        bigint id PK
        bigint equipment_id FK
        bigint equipment_owner_id FK
        bigint recorded_by_user_id FK
    }

    expenses {
        bigint id PK
        bigint created_by_id FK
        bigint recurring_source_id
    }

    recurring_expenses {
        bigint id PK
        bigint created_by_id FK
    }

    client_reviews {
        bigint id PK
        bigint client_id FK
        bigint author_id FK
    }

    document_templates {
        bigint id PK
        bigint created_by_id FK
    }

    callback_requests {
        bigint id PK
        bigint client_id FK
        bigint equipment_id FK
        bigint creator_user_id FK
    }
```

## Таблицы связей M:N

| Таблица | Колонки |
|---------|---------|
| `rental_equipment` | `rental_id` → `rentals.id`, `equipment_id` → `equipment.id` |
| `booking_equipment` | `booking_id` → `bookings.id`, `equipment_id` → `equipment.id` |

## Примечания

- У всех сущностей есть поля **`version`** (оптимистическая блокировка) и обычно **`is_deleted`** / флаги мягкого удаления — на диаграмме не показаны.
- В **`expenses.recurring_source_id`** хранится идентификатор строки **`recurring_expenses`** без JPA-`@ManyToOne` (слабая связь).
