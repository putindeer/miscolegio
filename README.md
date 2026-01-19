# Miscolegio

Plugin de Minecraft (Spigot/Paper) para un evento de preguntas por rondas con categorías por nivel, comodines y sistema de eliminación. (1.21+)

![License](https://img.shields.io/github/license/putindeer/woof-quests?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/putindeer/woof-quests?style=flat-square)

## Compilación

```bash
mvn clean package
```

## Comandos

Todos los comandos requieren el permiso `miscolegio.staff`:

- `/question` → Gestiona preguntas.
  - `/question create <id>` → Crea una pregunta con el ID deseado.
  - `/question list` → Muestra todas las preguntas junto a su respuesta correcta y sus datos específicos.
  - `/question delete <id>` → Borra una pregunta con el ID deseado.
  - `/question reload` → Recarga las preguntas desde questions.csv.
- `/zone` → Gestiona zonas del juego.
  - `/zone override` → Reemplaza la sesión activa si otro jugador la tiene abierta.
  - `/zone cancel` → Cancela tu selección actual.
  - `/zone delete` → Elimina la zona guardada del config.
  - `/zone reload` → Recarga la zona desde el config.
  - `/zone check` → Indica en qué zona estás actualmente.
  - `/zone bano` → Inicia la selección del punto de baño. (IMPORTANTE para el comodin del baño) 
  - `/zone banoconfirm` → Confirma el punto de baño actual.
- `/start` → Inicia el juego.
- `/forcestop` → Detiene el juego.
- `/reload` → Recarga la configuración del plugin (`config.yml`).

## Configurar la zona de juego

Puedes configurar la zona completa del juego desde el servidor con el comando `/zone`:

1. Ejecuta `/zone setup`.
2. Selecciona las dos esquinas con clic izquierdo y derecho para cada sección, en este orden:
   1. Respuesta A
   2. Respuesta B
   3. Respuesta C
   4. Respuesta D
   5. Área de juego
3. Tras definir ambas posiciones, usa `/zone confirm` para pasar a la siguiente sección.
4. Al finalizar se guarda en `config.yml`.
5. Usa el comando `/zone bano` para iniciar la selección de la zona del baño (es decir, donde irán los jugadores cuando usan el comodín, idealmente se debería elegir una zona fuera de la zona de juego).
6. Usa el comando `/zone banoconfirm` para establecer tu punto como la selección de la zona del baño.
7. ¡Ya está configurado!

## Preguntas (`questions.csv`)

Al iniciar el servidor se crea el archivo `questions.csv` con el siguiente encabezado:

```
id,question,optionA,optionB,optionC,optionD,answer,level,guaranteed
```

- **answer**: letra correcta (A/B/C/D).
- **level**: `KINDER`, `BASICA`, `MEDIA`, `UNIVERSIDAD`.
- **guaranteed**: `true/false`. (Si la pregunta debe estar garantizada en la pool. Si el número de preguntas garantizadas de una categoría es mayor que el de las preguntas por categoría, se toma en cuenta como número de preguntas por categoria el número de garantizadas.)

> Tip: puedes recargar el archivo con `/question reload`.

Comandos útiles:

- `/zone override` → Reemplaza la sesión activa si otro jugador la tiene abierta.
- `/zone cancel` → Cancela tu selección actual.
- `/zone delete` → Elimina la zona guardada del config.
- `/zone reload` → Recarga la zona desde el config.
- `/zone check` → Indica en qué zona estás actualmente.
- `/zone bano` → Inicia la selección del punto de baño. (IMPORTANTE para el comodin del baño) 
- `/zone banoconfirm` → Confirma el punto de baño actual.

## Configuración (`config.yml`)

```yml
game:
  # Tiempo para responder la pregunta (en segundos)
  question-time: 20

  # Cantidad de vidas que tiene el jugador
  lives: 3

  # Cuántas preguntas se toman por categoría
  questions-per-category:
    kinder: 7
    basica: 10
    media: 10
    universidad: 10

  # Colores de cada respuesta
  colors:
    A: RED
    B: GREEN
    C: BLUE
    D: YELLOW

  # Chances de comodín (en %)
  comodin-chances:
    C: 50
    B: 25
    A: 15
    S: 10

  # Cada cuántas rondas hay un comodín
  rounds-per-comodin: 5

  # Cuánto tiempo tarda en mostrar la eliminación del jugador
  eliminated-delay: 3.0

  # Cuántos comodines puede tener un jugador
  comodin-limit: 2

  # Solo dejar en 'true' si se está probando algo
  testing: false
```

## Mejoras (pendiente / en roadmap)

La idea es hacer el plugin más flexible con configuración adicional para:

- **Tablist**: Header/footer dinámicos y textos configurables.
- **Scoreboard**: Título, líneas y orden configurables.
- **Comodines**: Añadir más y permitir activarlos y desactivarlos a gusto.
- **Preguntas**: Posibilidad de renombrar o personalizar las categorías sin tocar el código.
- **Baño**: Hacer que configurarlo sea obligatorio para no añadir un paso supuestamente "opcional" que no lo es.
