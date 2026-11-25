# Spec Delta

## Purpose

Generar, hashear y comparar los tokens secretos que le dan al paciente acceso sin login a su turno (RN-07, RN-25) y a una oferta de hueco de la lista de espera (RN-38), sin que el valor en claro se pueda adivinar, reconstruir desde la base ni filtrar al convertirse en texto.

## ADDED Requirements

### Requirement: Generación de tokens de 256 bits
El sistema SHALL generar cada token con un generador de números aleatorios criptográficamente seguro, a partir de 256 bits (32 bytes) de entropía, codificado en base64url sin relleno: exactamente 43 caracteres del alfabeto `A-Z`, `a-z`, `0-9`, `-` y `_` (RN-25, DD-06).

#### Scenario: Un token generado tiene 43 caracteres del alfabeto base64url
- **WHEN** se genera un token
- **THEN** su valor en claro tiene exactamente 43 caracteres
- **AND** todos sus caracteres pertenecen al alfabeto base64url (`A-Z`, `a-z`, `0-9`, `-`, `_`), sin `=` de relleno

#### Scenario: Un token generado decodifica a 32 bytes
- **WHEN** se genera un token y se decodifica su valor en claro como base64url
- **THEN** se obtienen exactamente 32 bytes

#### Scenario: Diez mil tokens generados son todos distintos
- **WHEN** se generan 10.000 tokens
- **THEN** los 10.000 valores en claro son distintos entre sí
- **AND** los 10.000 hashes son distintos entre sí

### Requirement: El token generado incluye su hash
Al generar un token, el sistema SHALL entregar juntos el valor en claro (que se envía una sola vez al paciente) y su hash, para que el consumidor persista solo el hash (RN-25, RN-38).

#### Scenario: El token generado trae el hash de su valor en claro
- **WHEN** se genera un token
- **THEN** el hash que lo acompaña es igual al hash calculado a partir de su valor en claro

#### Scenario: Un token generado rechaza valores nulos
- **WHEN** se arma un token generado con el valor en claro o el hash nulo
- **THEN** se rechaza con un error de argumento inválido

### Requirement: Hash SHA-256 hexadecimal determinista
El sistema SHALL calcular el hash de un token como SHA-256 sobre sus bytes UTF-8, expresado en 64 caracteres hexadecimales en minúscula; el mismo token SHALL producir siempre el mismo hash. Un valor nulo o en blanco MUST rechazarse sin incluir el valor en el mensaje de error.

#### Scenario: El hash de un token es siempre el mismo
- **WHEN** se calcula dos veces el hash del mismo token
- **THEN** ambos resultados son iguales
- **AND** tienen 64 caracteres hexadecimales en minúscula

#### Scenario: El hash coincide con el vector conocido de SHA-256
- **WHEN** se calcula el hash del texto `abc`
- **THEN** el resultado es `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`

#### Scenario: Tokens distintos producen hashes distintos
- **WHEN** se calcula el hash de dos tokens distintos
- **THEN** los hashes son distintos

#### Scenario: Hashear un valor nulo o en blanco se rechaza
- **WHEN** se pide el hash de un valor nulo, vacío o solo con espacios
- **THEN** se rechaza con un error de argumento inválido

### Requirement: Comparación en tiempo constante
El sistema SHALL decidir si un token en claro corresponde a un hash guardado comparando los hashes con una primitiva de tiempo constante, cuyo tiempo no depende de cuántos caracteres coinciden (RN-25). Ante un token o un hash nulo, vacío o de otra longitud MUST responder que no coincide, sin lanzar error.

#### Scenario: Un token coincide con su propio hash
- **WHEN** se compara un token generado con el hash que lo acompaña
- **THEN** la comparación indica que coincide

#### Scenario: Un token no coincide con el hash de otro token
- **WHEN** se compara un token con el hash de otro token distinto
- **THEN** la comparación indica que no coincide

#### Scenario: Un hash nulo o de otra longitud no coincide
- **WHEN** se compara un token con un hash nulo, vacío o de longitud distinta de 64
- **THEN** la comparación indica que no coincide, sin lanzar error

#### Scenario: Un token nulo o en blanco no coincide
- **WHEN** se compara un valor nulo, vacío o solo con espacios con un hash válido
- **THEN** la comparación indica que no coincide, sin lanzar error

#### Scenario: La comparación usa una primitiva de tiempo constante
- **WHEN** se inspecciona cómo se comparan los hashes
- **THEN** la comparación delega en la primitiva de tiempo constante del JDK (`MessageDigest.isEqual`)
- **AND** no usa la igualdad de cadenas (`String.equals`)

### Requirement: El token en claro no aparece en su representación de texto
La representación de texto de un token generado MUST NOT contener el valor en claro ni el hash, para que registrar el objeto en un log no filtre el token (RN-25: nunca se registra el token en logs).

#### Scenario: El texto del token generado no contiene el token en claro ni su hash
- **WHEN** se convierte a texto un token generado
- **THEN** el texto no contiene el valor en claro
- **AND** el texto no contiene el hash
- **AND** el texto muestra un marcador de valor oculto

### Requirement: Servicio de token independiente de frameworks
El código del servicio de token MUST NOT depender de Spring, JPA ni Hibernate, para que sea dominio puro testeable sin contexto y reutilizable por `reservapublica` y `listaespera` sin que una dependa de la otra.

#### Scenario: El paquete del token no depende de Spring, JPA ni Hibernate
- **WHEN** se analizan las dependencias de las clases del paquete del token
- **THEN** ninguna depende de clases de Spring, JPA ni Hibernate
