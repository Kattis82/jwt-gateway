# Gateway

## Om tjänsten

Gateway är frontendens enda ingång till systemet. Den vidarebefordrar anrop till Auth Service, Product Service och Order
Service och hanterar inloggningen med JWT i en HttpOnly-cookie (`access_token`). Gateway läser token från cookien och
skickar den vidare som `Authorization: Bearer <token>`. Tjänsten har ingen egen databas.

```
Frontend -> Gateway -> Auth Service / Product Service / Order Service
```

## Teknik

- Java 17
- Spring Boot (Spring Web)
- Maven

## Köra lokalt

### Krav

- Java 17 och Maven (eller `mvnw`/`mvnw.cmd`)
- Auth Service, Product Service och Order Service igång

### Miljövariabler

| Variabel              | Beskrivning                                                                                 |
|-----------------------|---------------------------------------------------------------------------------------------|
| `SERVER_PORT`         | Port (standard 9003)                                                                        |
| `AUTH_SERVER_URL`     | Adress till Auth Service (standard `http://localhost:8080`)                                 |
| `PRODUCT_SERVICE_URL` | Adress till Product Service (standard `http://localhost:8082`)                              |
| `ORDER_SERVICE_URL`   | Adress till Order Service (standard `http://localhost:8086`)                                |
| `COOKIE_SECURE`       | `false` lokalt, `true` i produktion (https)                                                 |
| `COOKIE_SAME_SITE`    | `Lax` lokalt. Olika domäner i produktion kräver `None` tillsammans med `COOKIE_SECURE=true` |

### Starta tjänsten

Tjänsten körs på port **9003**.

```bash
./mvnw spring-boot:run
```

På Windows:

```bash
mvnw.cmd spring-boot:run
```

## API / Endpoints

Alla sökvägar börjar med `/gateway`.

- `POST /auth/login` – loggar in och sätter JWT i HttpOnly-cookie
- `GET /auth/me` – returnerar inloggad användare (kräver cookie)
- `POST /auth/logout` – loggar ut och tar bort cookien
- `POST /users/register` – registrerar en ny användare
- `GET /products` – hämtar alla produkter (öppen)
- `GET /products/{id}` – hämtar en produkt (öppen)
- `POST /products` – skapar en produkt (kräver inloggning)
- `DELETE /products/{id}` – tar bort en produkt (kräver inloggning)
- `POST /orders` – skapar en order (kräver inloggning)
- `GET /orders` – hämtar kundens ordrar (kräver inloggning)
- `POST /orders/checkout` – skapar order och startar Stripe Checkout (kräver inloggning)

Skyddade anrop utan cookie ger `401 Unauthorized`.

## Övrigt

- Stripes webhook går **inte** via Gateway. Stripe anropar Order Service direkt (`/webhooks/stripe`).
- JWT sparas aldrig i webbläsarens lagring, bara i HttpOnly-cookien.
- Gateway kontrollerar bara att cookien `access_token` finns. JWT:n valideras av tjänsten som tar emot anropet.
- Cookie-inställningarna (`COOKIE_SECURE`, `COOKIE_SAME_SITE`) måste anpassas när frontend och Gateway ligger på olika
  domäner.
- Swagger UI: `http://localhost:9003/swagger-ui/index.html`

## Länkar till övriga repositories

- Auth Service: https://github.com/J0ssan-iths/Auth-serverProject.git
- Product Service: https://github.com/JohannaVii/product-service.git
- Order Service: https://github.com/Kattis82/order-service.git
- Frontend: https://github.com/JohannaVii/Frontend.git

## Driftsatt miljö (Render)

- Auth Service: https://auth-serverproject-1-0-0.onrender.com
- Product Service: https://product-service-1-0-0-16ub.onrender.com
- Gateway: https://gateway-tyjw.onrender.com
- Order Service: https://order-service-1-0-0.onrender.com
- Frontend: https://webshoppen-kjpj.onrender.com


