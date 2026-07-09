# [Nombre] [Carné]

## Indicaciones

Recientemente, se utilizó AI para crear un sistema de gestion de una biblioteca, el cual ha generado varios errores, su trabajo es arreglarlo. Dado el siguiente caso de uso, explique y/o resuelva cada problema según se le pida.

---

## Consideraciones

La libreria crea automaticamente un correo con los nombres de la persona

---

## Problemas

### 1. Filtro por autor y género (10%)

QA ha reportado que el endpoint para obtener los libros puede filtrar por **autor** y por **género**, o por cualquiera de los dos de manera individual.

Actualmente:

- Filtrar únicamente por autor funciona correctamente.
- Filtrar únicamente por género funciona correctamente.
- Filtrar por **autor y género al mismo tiempo** provoca que el servidor falle.

**Instrucción:** Explique la causa del problema y resuélvalo.

genre era String, pero el campo Book.genre es un enum. Spring Data comparaba la columna enum contra un parámetro String, y eso generaba el conflicto a la hora de utilizar múltiples parámetros. La solución fue cambiar los tipados de String genre a Genre gener. Y adicionalmente cambiar el orden de algunos parámetros en las funciones.

---

### 2. Error al volver a prestar un libro (10%)

Un usuario reportó que al pedir prestado el libro **The Selfish Gene**, devolverlo e intentar pedirlo prestado nuevamente, el servidor falla.

**Instrucción:** Explique la causa del problema y resuélvalo.

El error que encontré es que no determinaba cuando un libro había sido devuelto, es decir, no se llevaba un registro de devoluciones y préstamos, entonces aunque se devolviera, no se sabía exactamente si el libro estaba disponible o no. 
Por lo que creé una interfaz para llevar un orden de las veces que había sido devuelto, y a partir de ahí validar que el libro y lector existan, verificar el stock (estos son validaciones adicionales) y por último actualizar en el momento nuevamente del estado del libro para permitir que pueda volver a ser prestado.

---

### 3. Cantidad de libros por género (10%)

Existe un endpoint que devuelve la cantidad de libros disponibles por género. Sin embargo, actualmente dicho endpoint falla.

**Instrucción:** Explique la causa del problema y resuélvalo.

No funcionaba por la misma diferencia de tipado que tenía el DTO en el atributo de género, al cambiar el tipo de String a Genre, y corregir el servicio, acoplándolo al nuevo tipado, funcionó el endpoint.

---

### 4. Error al consultar un libro por ID (10%)

Un miembro del equipo de frontend reporta que la siguiente llamada falla:

```http
GET /books?id=ed16ed1e-7017-4697-a08a-d28c09a74acf
```

**Instrucción:** Explique la causa del problema.

Porque el endpoint tiene los parámetros como que no son requeridos, entonces, como este llama al getAllBooks service, siempre devuelve todos los libros, pero cuando se parametriza con algún otr valor que no sean los específicados (author, genre), este simplemente no filtra nada y devuelve todos los libros.

Esto además funciona porque el service tiene las validaciones específicas para buscar por autor y género si no llegan como null, para buscar por autor o por género si no llega uno u otro null, o devolver todos simplemente, como fue mencionado anteriormente.

---

### 5. Error al crear un libro (10%)

QA ha reportado que el siguiente payload enviado al endpoint `POST /books` provoca un error:

```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "genre": "classic",
  "isbn": "978-0132350884",
  "available": true,
  "availableCount": 5
}
```

**Instrucción:** Explique la causa del problema.

Porque el enum del Genre tiene los valores en mayúsculas, entonces al poner "classic" en vez de "CLASSIC", da error y es como que no encuentra un valor válido al ser sensible a mayúsculas y minúsculas.

---

### 6. Devolución de libros no prestados (20%)

QA ha reportado que un usuario es capaz de devolver libros que nunca ha solicitado en préstamo.

**Instrucción:**

- Confirme si este comportamiento es realmente posible. 
- Si es posible, explique la causa y resuelva el problema. 
- Si no es posible, explique por qué, haciendo referencia al código correspondiente. 


Si, el comportamiento es posible, cree un usuario completamente nuevo y traté de devolver un libro cualquiera y si funcionó.
Si fue posible corregir el error, fue seguir el mismo procedimiento al corregir el endpoint de prestar un libro ya que antes returnBook era return null; sin ninguna validación por lo que cualquier ISBN + correo válidos devolvía el libro sin comprobar historial. Ahora se revisa al último lector y se comprueba si ya ha sido devuelto o si tan siquiera ha prestado el libro
   
---
