# Dev Orus 1.1 — Complete Python Handbook (Beginner to Advanced)

**Edition:** 1.0  
**Language scope:** Python only  
**Compatibility baseline:** Python 3.11+ unless a section says otherwise.  
**Purpose:** A self-contained reference and teaching corpus. It uses the Python standard library first; optional third-party packages are identified as optional.  
**Important training note:** This handbook adds learning material and retrieval knowledge. It does not, by itself, update neural-network weights or prove that the model has mastered Python.

---

## 0. How to use this handbook

Read the chapters in order, run every example, change values, predict outputs before running code, and complete the exercises. A topic is not mastered just because its explanation was read. Mastery requires writing and debugging unseen programs.

A useful practice loop:

1. Read the concept and its limitations.
2. Predict the output of the example without running it.
3. Run it in a Python interpreter.
4. Change one part and explain the difference.
5. Write a similar program from a blank file.
6. Test normal, boundary and invalid inputs.

## 1. Python, interpreter and execution model

Python is a high-level, general-purpose language. The language defines syntax and behavior; a Python implementation executes it. CPython is the most common implementation. Other implementations exist and can differ in performance or supported integrations.

A source file commonly uses the `.py` extension. Run it with:

```bash
python3 app.py
```

Interactive mode (REPL) evaluates expressions as they are entered:

```python
>>> 2 + 3
5
```

In a script, a bare expression normally does not display its value. Use `print()` for output.

Python source is compiled to bytecode by CPython and then executed by its virtual machine. This is an implementation detail; do not assume bytecode is identical across Python versions or implementations.

## 2. Syntax, indentation, comments and names

A colon introduces a compound statement's suite; indentation defines the block. Four spaces per level is the conventional style.

```python
if True:
    print("inside")
print("outside")
```

Use `#` for a single-line comment. Triple-quoted strings are string literals often used as docstrings; they are not a special comment syntax.

Names are case-sensitive. Prefer descriptive `snake_case` for variables and functions, `PascalCase` for classes, and uppercase names for module-level constants by convention.

```python
student_count = 24
MAX_RETRIES = 3

def calculate_total(price, quantity):
    return price * quantity

class ShoppingCart:
    pass
```

A name is a binding to an object, not a box that permanently owns a type.

## 3. Variables, objects, identity and mutability

Python is dynamically typed. Objects have types; names can be rebound to objects of different types.

```python
value = 10
value = "ten"
```

Assignment binds a name. It does not necessarily copy an object.

```python
first = [1, 2]
second = first
second.append(3)
print(first)  # [1, 2, 3]
```

Both names refer to the same list. Use `copy.copy()` for a shallow copy or `copy.deepcopy()` for a recursive deep copy when appropriate. Deep copying is not always correct for objects containing files, sockets, locks or shared resources.

Mutable objects can change in place; immutable objects such as integers, strings and tuples cannot be changed in place. A tuple can still contain a mutable object:

```python
record = ("Asha", [80, 90])
record[1].append(95)  # allowed: the list inside is mutable
```

Use `is` for identity (whether two references point to the same object), and `==` for value equality. Use `is None` to test for None.

## 4. Built-in types and conversions

Important built-in types:

- Numeric: `int`, `float`, `complex`, `bool`
- Null-like value: `None`
- Text and binary: `str`, `bytes`, `bytearray`
- Collections: `list`, `tuple`, `dict`, `set`, `frozenset`
- Iteration helpers: `range`, iterators and generators

Convert deliberately:

```python
count = int("42")
price = float("19.5")
label = str(100)
raw = bytes([65, 66, 67])
```

Conversions can fail: `int("hello")` raises `ValueError`. Do not assume data from a user, file or network is valid.

Truth-value testing treats values such as `None`, `False`, numeric zero and empty collections as false. Most other objects are true. Prefer explicit checks when the distinction matters:

```python
if items is None:
    print("not supplied")
elif not items:
    print("supplied but empty")
```

## 5. Operators and expressions

Arithmetic: `+`, `-`, `*`, `/`, `//`, `%`, `**`.  
Comparison: `==`, `!=`, `<`, `<=`, `>`, `>=`.  
Boolean: `and`, `or`, `not`.  
Membership: `in`, `not in`.  
Identity: `is`, `is not`.  
Bitwise: `&`, `|`, `^`, `~`, `<<`, `>>`.

`/` performs true division; `//` floors the quotient. For negative numbers, floor division rounds toward negative infinity:

```python
print(7 / 2)    # 3.5
print(7 // 2)   # 3
print(-7 // 2)  # -4
print(7 % 2)    # 1
print(2 ** 3)   # 8
```

Boolean operators short-circuit and return one of their operands, not necessarily a bool:

```python
name = supplied_name or "Guest"
```

Avoid relying on complicated precedence. Add parentheses to make intent clear.

## 6. Strings, Unicode and formatting

Strings are immutable sequences of Unicode code points.

```python
message = "Python"
print(message[0])     # P
print(message[1:4])   # yth
print(message[::-1])  # nohtyP
```

Common methods include `strip`, `split`, `join`, `replace`, `startswith`, `endswith`, `lower` and `casefold`. `casefold()` is useful for caseless matching but does not perform every kind of language-aware comparison.

Use f-strings for readable formatting:

```python
name = "Asha"
score = 92.456
print(f"{name}: {score:.1f}")
```

Output: `Asha: 92.5`.

Text and bytes are different. Encode text to bytes and decode bytes to text using a known encoding, commonly UTF-8:

```python
encoded = "नमस्ते".encode("utf-8")
decoded = encoded.decode("utf-8")
```

Never guess an encoding when correctness matters.

## 7. Collections: list, tuple, dict and set

Lists are ordered and mutable:

```python
tasks = ["read", "test"]
tasks.append("ship")
tasks[0] = "study"
```

Tuples are immutable sequences, often used for fixed records:

```python
point = (4, 7)
x, y = point
```

Dictionaries map hashable keys to values and preserve insertion order:

```python
profile = {"name": "Asha", "level": 2}
profile["level"] += 1
profile["city"] = "Lucknow"
```

Sets hold unique hashable elements:

```python
unique = {2, 2, 3, 4}
print(unique)  # {2, 3, 4} (display order is not a promise to rely on)
```

Use `dict.get(key, default)` for optional lookups. A missing key with square-bracket access raises `KeyError`. Do not mutate a collection while iterating over it unless the algorithm explicitly accounts for that; often iterate over a copy or build a new collection.

## 8. Slicing, unpacking and comprehensions

Slicing uses `sequence[start:stop:step]`; stop is excluded.

```numbers = [10, 20, 30, 40, 50]
print(numbers[1:4])  # [20, 30, 40]
first, *middle, last = numbers
```

Comprehensions create collections concisely:

```squares = [n * n for n in range(6)]
even_squares = {n * n for n in range(10) if n % 2 == 0}
lookup = {n: n * n for n in range(4)}
```

A generator expression is lazy:

```total = sum(n * n for n in range(1_000_000))
```

Prefer clarity over deeply nested comprehensions.

## 9. Control flow and pattern matching

`if/elif/else` selects branches. `for` iterates over an iterable; `while` repeats while a condition is true.

```for index, item in enumerate(["a", "b"], start=1):
    print(index, item)

for left, right in zip([1, 2], [3, 4]):
    print(left + right)
```

Use `break` to leave a loop, `continue` to skip to its next iteration, and loop `else` when the loop completed without `break`.

Structural pattern matching (Python 3.10+) can match shapes:

```def describe(command):
    match command:
        case ("move", x, y):
            return f"Move to {x}, {y}"
        case ("quit",):
            return "Quit"
        case _:
            return "Unknown command"
```

Use pattern matching when it improves clarity; ordinary conditionals remain appropriate.

## 10. Functions and parameter semantics

Functions package reusable behavior. Parameters are names in a function definition; arguments are values supplied by a call.

```def area(width: float, height: float = 1.0) -> float:
    """Return rectangle area."""
    return width * height

print(area(5))
print(area(height=3, width=4))
```

Python supports positional-only parameters (before `/`), positional-or-keyword parameters, keyword-only parameters (after `*`), `*args` and `**kwargs`.

```def build_url(host, /, path, *, secure=True):
    scheme = "https" if secure else "http"
    return f"{scheme}://{host}/{path.lstrip('/')}"

def inspect_values(*values, **options):
    return values, options
```

Avoid mutable default arguments. Defaults are evaluated once when the function is defined:

```def add_item(item, items=None):
    if items is None:
        items = []
    items.append(item)
    return items
```

A function without an explicit return returns `None`. Return values are not the same as printed output.

## 11. Scope, closures and recursion

Python resolves local names using the LEGB rule: Local, Enclosing, Global, Built-in. `global` and `nonlocal` change where assignment binds; use them sparingly.

A closure retains access to variables from an enclosing scope:

```def make_multiplier(factor):
    def multiply(value):
        return value * factor
    return multiply

double = make_multiplier(2)
print(double(8))  # 16
```

Recursion is a function calling itself. It needs a base case and progress toward that case:

```def factorial(n):
    if n < 0:
        raise ValueError("n must be non-negative")
    if n < 2:
        return 1
    return n * factorial(n - 1)
```

Python has a recursion limit; for large inputs, an iterative algorithm is often safer.

## 12. Functional tools and sorting

Useful built-ins include `sorted`, `any`, `all`, `sum`, `min`, `max`, `enumerate`, `zip`, `map` and `filter`.

```students = [
    {"name": "Ravi", "score": 81},
    {"name": "Asha", "score": 94},
]
ranked = sorted(students, key=lambda student: student["score"], reverse=True)
```

`sorted()` returns a new list; `list.sort()` changes a list in place. A sorting key should return a comparable value. Prefer a named function when a lambda becomes difficult to read.

## 13. Modules, packages and imports

A module is an importable unit, usually a `.py` file. A package organizes modules in a directory. Importing a module executes its top-level statements once per interpreter process (subject to the import cache).

```python
import math
from pathlib import Path

print(math.sqrt(81))
print(Path("notes.txt").suffix)
```

Keep import-time work small. Put program startup behind:

```def main():
    print("Application started")

if __name__ == "__main__":
    main()
```

This allows a module to be imported without automatically running its command-line behavior. Avoid naming your files after standard-library modules, such as `json.py` or `random.py`, because they can shadow the real modules.

## 14. Exceptions and error design

Exceptions report errors or exceptional control flow. Catch the narrowest useful exception and handle it where recovery is possible.

```try:
    quantity = int("12")
except ValueError as exc:
    print(f"Invalid quantity: {exc}")
else:
    print("Quantity:", quantity)
finally:
    print("Validation finished")
```

Raise clear errors for invalid function arguments:

```def percentage(value, total):
    if total <= 0:
        raise ValueError("total must be positive")
    return value / total * 100
```

Define custom exceptions when callers need to distinguish domain-specific failures. Use `raise NewError(...) from exc` to preserve causal context. Do not use bare `except:` to hide bugs, and do not silently discard unexpected failures.

## 15. Filesystem and file I/O

Use `pathlib.Path` for path operations:

```from pathlib import Path

path = Path("data") / "message.txt"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text("Hello\n", encoding="utf-8")
text = path.read_text(encoding="utf-8")
```

Use `with` so files are closed reliably. Choose text or binary mode intentionally. Specify encodings for text. For large files, stream line by line instead of reading everything into memory.

```with open("large.log", encoding="utf-8") as handle:
    for line in handle:
        process_line = line.rstrip("\n")
        # process_line is one line at a time
```

Treat untrusted paths carefully. Resolve paths and ensure they remain inside the intended base directory before writing. Do not let user-controlled paths overwrite arbitrary files.

## 16. Context managers

A context manager sets up and reliably cleans up a resource. The `with` statement calls its context protocol methods.

```from contextlib import contextmanager

@contextmanager
def managed_message():
    print("start")
    try:
        yield "resource"
    finally:
        print("cleanup")

with managed_message() as item:
    print(item)
```

Use context managers for files, locks, database transactions and other resources that need predictable cleanup. Prefer existing context managers instead of writing custom ones unnecessarily.

## 17. JSON, CSV and data interchange

JSON supports objects, arrays, strings, numbers, booleans and null. Python's `json` module converts between JSON text and Python values:

```import json

record = {"name": "Asha", "active": True}
text = json.dumps(record, ensure_ascii=False)
restored = json.loads(text)
```

CSV is tabular text. Use the `csv` module rather than manually splitting on commas; quoted fields can contain commas and newlines.

```import csv

with open("people.csv", newline="", encoding="utf-8") as handle:
    for row in csv.DictReader(handle):
        print(row["name"])
```

Validate schema, types, size and expected fields after parsing. Parsing successfully does not mean the data is trustworthy.

## 18. Iterables, iterators and generators

An iterable can produce an iterator. An iterator implements `__iter__()` and `__next__()`; exhaustion raises `StopIteration`.

```values = [4, 5]
iterator = iter(values)
print(next(iterator))
print(next(iterator))
```

A generator function uses `yield` and returns a lazy iterator:

```def read_chunks(items, size):
    for start in range(0, len(items), size):
        yield items[start:start + size]
```

Generators are useful for pipelines and large data because they produce values on demand. They are generally one-pass. Do not assume a generator can be restarted.

## 19. Object-oriented programming

A class defines behavior and data; an instance is an object created from the class.

```class Account:
    def __init__(self, owner, balance=0):
        self.owner = owner
        self._balance = balance

    @property
    def balance(self):
        return self._balance

    def deposit(self, amount):
        if amount <= 0:
            raise ValueError("amount must be positive")
        self._balance += amount
```

`self` is the conventional name for the instance parameter. `__init__` initializes an already-created instance; it is not the allocator. A leading underscore communicates non-public implementation detail by convention, not enforced privacy.

Use instance methods for instance behavior, `@classmethod` for class-oriented constructors or behavior, and `@staticmethod` for a function namespaced in the class that needs neither instance nor class.

Prefer composition when one object can use another object without inheriting from it. Inheritance is useful for genuine substitutability, not merely code reuse. Understand method resolution order (MRO) before using multiple inheritance.

## 20. Dataclasses, enums, properties and protocols

Dataclasses reduce boilerplate for data-centric classes:

```from dataclasses import dataclass

@dataclass(frozen=True)
class Point:
    x: float
    y: float
```

Use `field(default_factory=list)` for mutable dataclass defaults. `frozen=True` prevents normal field reassignment but does not recursively freeze referenced mutable objects.

Use `Enum` for a fixed set of named values. Use properties when attribute access needs validation or computed behavior. Descriptors customize attribute access at a lower level; use them only when the abstraction is justified.

Protocols describe behavior expected from an object (structural typing). Abstract base classes can enforce an explicit inheritance contract. Duck typing means code relies on supported behavior rather than exact concrete class identity.

## 21. Special methods and Python's data model

Special methods such as `__len__`, `__iter__`, `__getitem__`, `__enter__`, `__exit__`, `__eq__` and `__repr__` integrate custom objects with language operations.

```class Playlist:
    def __init__(self, songs):
        self._songs = list(songs)

    def __len__(self):
        return len(self._songs)

    def __iter__(self):
        return iter(self._songs)

    def __repr__(self):
        return f"Playlist({self._songs!r})"
```

Implement only the protocols your class needs, and keep their semantics unsurprising. Equality and hashing must agree: objects that compare equal should have equal hashes. Mutable objects should generally not be hash keys.

## 22. Decorators and descriptors

A decorator receives a callable and returns a replacement callable or another object.

```from functools import wraps

def announce(function):
    @wraps(function)
    def wrapper(*args, **kwargs):
        print("Calling", function.__name__)
        return function(*args, **kwargs)
    return wrapper

@announce
def greet(name):
    return f"Hello, {name}"
```

Use `functools.wraps` to preserve useful metadata. Decorators can affect signatures, debugging and call behavior; keep them transparent. Parameterized decorators are typically functions that return decorators. Descriptors implement `__get__`, `__set__` or `__delete__` and underpin properties and many ORM-style APIs.

## 23. Type hints and static analysis

Type hints document expected types and help static analyzers. Python does not automatically enforce most annotations at runtime.

```def first_item(items: list[str]) -> str | None:
    return items[0] if items else None
```

Useful typing concepts include `Any`, `object`, `Optional` (or `T | None`), `Union`, `Literal`, `TypedDict`, `Protocol`, `Callable`, generics and type variables. Prefer `object` when a value is unknown but should still be treated safely; `Any` disables many checks.

Type hints should reflect actual behavior. They do not replace runtime validation of untrusted data. Tools such as mypy and pyright are optional third-party static analyzers, not part of the Python runtime.

## 24. Standard library essentials

Learn these standard-library modules before adding dependencies:

- `pathlib`: filesystem paths.
- `os`, `sys`: operating-system and interpreter interfaces.
- `collections`: Counter, defaultdict, deque and namedtuple.
- `itertools`: efficient iterator building blocks.
- `functools`: caching, partial application and wrapping.
- `math`, `statistics`: numeric functions.
- `datetime`, `zoneinfo`: dates, times and time zones.
- `re`: regular expressions.
- `json`, `csv`: common data formats.
- `argparse`: command-line arguments.
- `logging`: application diagnostics.
- `sqlite3`: embedded SQL database.
- `unittest`: standard testing framework.
- `subprocess`: launching external programs with care.
- `tempfile`: temporary files and directories.
- `hashlib`, `secrets`: hashes and secure random values for appropriate uses.

For timestamps, be explicit about time zones. A naive datetime does not identify a unique global moment.

## 25. Regular expressions

Regular expressions search and transform text patterns. Use raw strings for patterns:

```import re

match = re.fullmatch(r"[A-Z]{2}[0-9]{4}", "AB1234")
print(bool(match))
```

Use `search` to find a match anywhere, `match` to match at the start, and `fullmatch` to require the entire string. For complex validation, a dedicated parser may be clearer. Be cautious with catastrophic backtracking on untrusted long input.

## 26. Command-line applications and configuration

Use `argparse` to parse command-line arguments:

```import argparse

parser = argparse.ArgumentParser(description="Count words in a file")
parser.add_argument("filename")
args = parser.parse_args()
```

Separate argument parsing, application logic and output. Return meaningful exit codes. Keep secrets out of source code and logs. Configuration can come from explicit command-line options or environment variables; validate it before use.

## 27. Testing and test-driven development

A test checks expected behavior. Test normal cases, edge cases, invalid input and regressions.

```import unittest

def add(a, b):
    return a + b

class AddTests(unittest.TestCase):
    def test_positive_numbers(self):
        self.assertEqual(add(2, 3), 5)

    def test_negative_number(self):
        self.assertEqual(add(-2, 1), -1)

if __name__ == "__main__":
    unittest.main()
```

Run with `python -m unittest`. Pytest is a popular optional third-party framework. Use mocks at boundaries such as network services or clocks; do not mock every internal detail. Tests should be deterministic and independent.

## 28. Debugging, logging and code quality

Read tracebacks from the exception at the bottom and follow the call stack to the relevant source. Use a debugger or `breakpoint()` to inspect state. Remove accidental breakpoints before release.

Use `logging` rather than scattered debug prints in long-running applications:

```import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logger.info("Application ready")
```

Do not log passwords, access tokens, private user data or sensitive payloads. Format code consistently, use descriptive names, keep functions focused and document public interfaces. Linters and formatters are optional tools; review their output rather than blindly accepting every change.

## 29. Algorithms, complexity and performance

Big-O describes how resource use grows with input size. A list membership check is generally O(n); set or dictionary membership is average-case O(1), with implementation-dependent caveats. Sorting is generally O(n log n).

Choose clear algorithms first. Measure with `timeit` or profile with `cProfile` before optimizing. Avoid guessing where a program is slow. Generators can reduce peak memory; they do not automatically make every task faster.

## 30. Databases with sqlite3

SQLite is included in the standard library and stores data in a local database.

```import sqlite3

with sqlite3.connect("app.db") as connection:
    connection.execute(
        "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, body TEXT NOT NULL)"
    )
    connection.execute(
        "INSERT INTO notes(body) VALUES (?)",
        ("Learn Python",)
    )
```

Always use parameterized queries (the `?` placeholder above) for values. Never build SQL by concatenating untrusted input. Understand transactions, constraints, indexes and backups. The connection context manager commits on successful exit and rolls back on an exception.

## 31. Networking and HTTP

HTTP is a request/response protocol. Requests have methods, headers and sometimes bodies; responses have status codes, headers and bodies. Use a maintained HTTP client when the project permits dependencies; the standard library includes `urllib.request` for basic use.

Handle timeouts, status codes, redirects, encoding, response-size limits and network failures. Do not assume remote data is valid. Do not put credentials in URLs or print authorization headers. Follow the service's terms and rate limits.

## 32. Concurrency: threads, processes and the GIL

Concurrency means managing multiple tasks whose execution overlaps. Threads are useful for many I/O-bound tasks. Processes can use multiple CPU cores for CPU-bound work but have startup and data-transfer costs. The Global Interpreter Lock (GIL) in standard CPython builds historically limits simultaneous execution of Python bytecode in threads; Python version and build configuration matter, and free-threaded builds are evolving.

Use `threading` for suitable I/O coordination and `concurrent.futures` for a higher-level interface. Protect shared mutable state with synchronization primitives. Avoid deadlocks by designing lock order and lifetime carefully.

## 33. Asynchronous programming with asyncio

Asyncio is cooperative concurrency, often useful for many I/O-bound tasks. A coroutine is defined with `async def`; `await` suspends it while awaiting an awaitable.

```import asyncio

async def greet_after_delay():
    await asyncio.sleep(0.1)
    return "Hello"

async def main():
    result = await greet_after_delay()
    print(result)

asyncio.run(main())
```

Use tasks to run independent coroutines concurrently. Handle cancellation and timeouts; do not block the event loop with long synchronous operations. Asyncio does not automatically speed up CPU-heavy calculations. Use threads or processes where appropriate.

## 34. Packaging and environments

Create an isolated environment:

```bash
python3 -m venv .venv
```

Activate it using the platform-specific activation script, then install project dependencies with `python -m pip install package-name`. Prefer a project manifest such as `pyproject.toml`, declare dependencies and versions responsibly, and document reproducible setup. Do not commit the environment directory itself.

A wheel is a built distribution format. A source distribution contains source needed for building. Learn package metadata, versioning, entry points and build backends before publishing.

## 35. Security and robust Python

- Treat all external input as untrusted: command-line values, files, network responses and database data.
- Validate type, length, range and format at the boundary.
- Never use `eval()` or `exec()` on untrusted text.
- Do not unpickle untrusted data; pickle can execute code during loading.
- Use parameterized SQL queries.
- Avoid shell command strings built from user input. Prefer `subprocess.run([...], check=True, timeout=...)` with an argument list and a controlled environment.
- Use `secrets` for security-sensitive random tokens; the `random` module is not cryptographically secure.
- Store secrets outside source control and restrict access.
- Use safe path handling to prevent path traversal.
- Limit file, network and decompression sizes when processing untrusted content.
- Keep dependencies updated and review their provenance.

Example of safer subprocess argument passing:

```python
import subprocess

result = subprocess.run(
    ["python", "--version"],
    check=True,
    capture_output=True,
    text=True,
    timeout=5,
)
print(result.stdout or result.stderr)
```

## 36. Memory management and implementation details

In CPython, reference counting and a cyclic garbage collector are important implementation mechanisms. Other Python implementations may manage memory differently. Do not rely on immediate object destruction for resource cleanup; use context managers for files, locks and other external resources.

Understand shallow versus deep copies, object lifetime, references and cycles. Use `tracemalloc` to investigate Python allocation behavior. Memory views and bytearrays can help with binary data, but require careful ownership and mutation reasoning.

## 37. Extending Python and embedding

Python can interoperate with native code through extension mechanisms and embedding APIs. These involve ABI compatibility, memory ownership, reference counting, exception translation and build configuration. They are advanced integration topics, not a replacement for learning Python's language and standard library. Native extensions should be added only when a measured need justifies the complexity.

## 38. Project architecture and maintainability

A maintainable project separates responsibilities:

- Domain logic: core rules and calculations.
- I/O boundaries: files, databases and network.
- Interface: command-line, UI or API.
- Tests: verify behavior without depending on live external services.

Use dependency injection at boundaries when it makes tests simpler. Avoid giant files, circular imports, hidden global state and overly abstract designs. Start with the simplest structure that supports the requirements.

## 39. Capstone projects

Complete these in order, adding tests and documentation to each:

1. **Command-line calculator:** parse operations, validate input, handle division by zero and test boundaries.
2. **Text statistics tool:** count lines, words and characters while streaming large files.
3. **JSON notes manager:** add, list, update and remove notes with robust file handling.
4. **CSV report generator:** validate rows, calculate summaries and write output safely.
5. **SQLite task tracker:** parameterized SQL, transactions, constraints and tests.
6. **HTTP status checker:** timeouts, clear error handling, bounded response reading and respectful request behavior.
7. **Async task monitor:** concurrent I/O tasks, cancellation and timeouts.
8. **Packaged Python CLI:** pyproject metadata, entry point, tests and setup instructions.

For every capstone, provide a README, sample input/output, unit tests, invalid-input tests and a short design explanation.

## 40. Final mastery assessment

A learner should not be marked advanced until they can complete unseen tasks in all of these areas:

1. Explain names, references, mutability, identity and equality.
2. Predict expression results, including negative floor division and short-circuit behavior.
3. Choose appropriate collection types and explain their complexity trade-offs.
4. Write functions with correct parameter kinds and safe defaults.
5. Explain scope and implement a closure.
6. Build and import a small package without import side effects.
7. Raise and handle precise exceptions.
8. Process large text and CSV files without loading all data into memory.
9. Implement and consume an iterator or generator.
10. Design a class using composition, properties and appropriate special methods.
11. Add meaningful type hints and explain their runtime limits.
12. Write unit tests for normal, boundary and invalid cases.
13. Use SQLite parameter binding correctly.
14. Explain when to use threads, processes or asyncio.
15. Identify unsafe uses of eval, pickle, shell strings and unvalidated paths.
16. Build, document and test one complete capstone project.

**Suggested passing rule:** at least 80% overall, no critical security errors, and successful completion of a fresh capstone. Revisit weak topics and retest with different questions.

---

## Appendix A. Quick reference

```python
# Output
print("Hello")

# Assignment
count = 3

# Branch
if count > 0:
    print("positive")
else:
    print("not positive")

# Loop
for number in range(3):
    print(number)

# Function
def square(number):
    return number * number

# Exception handling
try:
    value = int("12")
except ValueError:
    value = 0

# File
from pathlib import Path
Path("hello.txt").write_text("Hello", encoding="utf-8")

# JSON
import json
encoded = json.dumps({"ok": True})
decoded = json.loads(encoded)

# Test
assert square(4) == 16
```

## Appendix B. Reliable learning references

Use the official Python documentation as the primary reference for exact version-specific behavior:
- Python Tutorial: https://docs.python.org/3/tutorial/
- Python Language Reference: https://docs.python.org/3/reference/
- Python Standard Library: https://docs.python.org/3/library/
- Python HOWTOs: https://docs.python.org/3/howto/

When behavior depends on a Python version or implementation, check the documentation for the target runtime rather than assuming every environment behaves identically.
