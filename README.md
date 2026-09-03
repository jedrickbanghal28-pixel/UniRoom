# UniRoom functional Android + PHP + PostgreSQL
Architecture: Android Java/XML -> Retrofit -> PHP REST API -> PDO -> PostgreSQL. pgAdmin 4 is used to view/manage PostgreSQL, not as the runtime database.

## Setup
1. In pgAdmin 4, open database `banghal` and run `database/uniroom_banghal.sql`.
2. In XAMPP start Apache and enable `pdo_pgsql` and `pgsql` in php.ini, then restart Apache. This follows the supplied lesson guide, which requires those extensions.
3. Copy `php_api` to `C:\xampp\htdocs\uniroom_api`.
4. Copy `config.example.php` to `config.php` and put your LOCAL PostgreSQL password in `config.php` only.
5. Test `http://localhost/uniroom_api/test_connection.php`.
6. Open this project in Android Studio and run the emulator. The Retrofit base URL is `http://10.0.2.2/uniroom_api/`.

## Required demo
- Valid registration -> success message -> pgAdmin SELECT shows inserted row.
- Empty required field -> Android field validation.
- Existing username/email -> PHP returns HTTP 409 and Android displays duplicate message.
- Login -> Dashboard -> Schedule / Find My Room / Profile.

Never put PostgreSQL credentials in Android Studio. The supplied lesson specifically says the safe path is Android -> HTTP -> PHP API -> PostgreSQL and that database passwords must not be placed in the Android APK. See pages 5, 16, 27 and 35 of the supplied guide.
