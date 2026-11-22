# Dev Orus 1.1 — Advanced Python Masterclass

This masterclass extends the complete Python handbook with deeper engineering concepts. It is reference and practice material, not evidence that neural-network weights have been updated. A topic is mastered only after the learner can explain it, implement it, test it, and solve an unseen problem.

## 1. Python execution and object semantics

Python source is parsed, compiled to code objects, and executed by an implementation such as CPython. CPython commonly executes bytecode in its virtual machine. Separate language guarantees from implementation details: PyPy and other implementations may work differently internally.

Names refer to objects. Assignment binds a name; it does not automatically copy an object. If two names refer to one mutable list, mutation through either name is visible through both. Every object has identity, type, and value. Use identity comparison for singleton checks such as x is None; use equality for value comparison.

~~~python
first = [1, 2]
alias = first
alias.append(3)
assert first == [1, 2, 3]
assert first is alias
copy = first.copy()
assert copy == first and copy is not first
~~~

Scope resolution follows LEGB: Local, Enclosing, Global, Builtins. global rebinds a module-level name; nonlocal rebinds a name in the nearest enclosing function scope. Prefer explicit arguments and return values over hidden global state.

## 2. Function design and parameter semantics

Functions are first-class objects: they can be stored, passed, returned, and decorated. Python supports positional-only parameters, positional-or-keyword parameters, keyword-only parameters, variadic positional arguments, and variadic keyword arguments.

~~~python
def connect(host, /, port=443, *, timeout=5.0, secure=True):
    return host, port, timeout, secure

connect("example.test", timeout=2)
~~~

Default values are evaluated once when the def statement executes. A mutable default can therefore be shared across calls. Use None as a sentinel when a fresh object is required.

~~~python
def add_item(item, items=None):
    if items is None:
        items = []
    items.append(item)
    return items
~~~

Closures retain references to enclosing variables. A common late-binding trap occurs when lambdas are created inside a loop: they may all observe the final loop value. Bind the current value in a default parameter or use a helper function. Recursion is useful for naturally recursive structures, but Python does not guarantee tail-call optimization; use iteration for deep workloads.

## 3. Decorators and functional tools

A decorator receives a function or class and returns a replacement. Preserve metadata with functools.wraps. Stacked decorators are applied from the bottom upward. Decorators with configuration need an outer function that returns the actual decorator.

~~~python
from functools import wraps
from time import perf_counter

def measure(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = perf_counter()
        try:
            return func(*args, **kwargs)
        finally:
            print(perf_counter() - start)
    return wrapper
~~~

Learn map, filter, sorted with key functions, any, all, enumerate, zip, functools.partial, lru_cache, singledispatch, operator, and itertools. Prefer a comprehension or ordinary loop when it communicates intent more clearly. Cache only when the function is suitably deterministic and the cache has a sensible lifetime and size.

## 4. Iteration protocol and generators

An iterable can produce an iterator. An iterator implements __iter__ returning itself and __next__ returning the next value or raising StopIteration. A generator function uses yield and executes lazily as values are requested.

~~~python
def nonempty(lines):
    for line in lines:
        value = line.strip()
        if value:
            yield value

words = (word.lower() for word in nonempty([" Hi ", "", "THERE"]))
assert list(words) == ["hi", "there"]
~~~

Generators are usually one-shot. yield from delegates iteration. Generator methods send, throw, and close support advanced coroutine-like protocols, although async and await are preferred for modern asynchronous I/O. Generator expressions are lazy; list comprehensions eagerly allocate a list. Choose based on memory, reuse, and clarity.

## 5. Context managers and resource safety

A context manager defines __enter__ and __exit__. The with statement ensures cleanup when control leaves the block, including when an exception occurs. Use context managers for files, locks, database transactions, and temporary resources.

~~~python
from contextlib import contextmanager

@contextmanager
def managed(open_resource, close_resource):
    resource = open_resource()
    try:
        yield resource
    finally:
        close_resource(resource)
~~~

Study contextlib.contextmanager, closing, suppress, ExitStack, and AsyncExitStack. ExitStack is useful when the number of resources is known only at runtime. Never rely on garbage-collection timing to close important resources.

## 6. Object model, inheritance, and composition

Attribute lookup involves instance attributes, class attributes, descriptors, and the method resolution order. Inheritance expresses a genuine is-a relationship. Composition expresses has-a and often reduces coupling. Prefer composition when subclasses need to override many parent methods or depend on fragile parent internals.

super() follows the method resolution order; it does not simply mean call my parent. Cooperative multiple inheritance requires classes to use super consistently and accept compatible arguments. Inspect a class's MRO to understand lookup order.

Use properties for a computed or validated attribute interface, class methods for alternative constructors, and static methods for namespaced utilities that do not need instance or class state. Avoid properties that secretly perform expensive network or database work.

## 7. Python data model and special methods

Python syntax often invokes special methods. len uses __len__; iteration uses __iter__ and __next__; arithmetic may use __add__; with uses context-manager methods. Implement special methods only when their semantics are consistent.

Important groups include representation (__repr__, __str__, __format__), comparisons and hashing (__eq__, ordering methods, __hash__), containers (__len__, __contains__, __getitem__, __setitem__, __iter__), numeric operations (__add__, __mul__, reflected and in-place variants), construction (__new__, __init__), and call/context protocols (__call__, __enter__, __exit__).

The equality/hash contract is essential: objects that compare equal must have equal hashes. Mutable objects should generally not be hashable if mutation can change equality. Return NotImplemented when an operation does not support the other operand type. Avoid relying on __del__ for critical cleanup.

## 8. Descriptors and attribute access

A descriptor defines __get__, __set__, or __delete__. Properties, methods, and many ORM fields use this protocol. Data descriptors generally take precedence over an instance dictionary; non-data descriptors can be overridden by instance attributes.

~~~python
class Positive:
    def __set_name__(self, owner, name):
        self.name = name

    def __get__(self, instance, owner=None):
        if instance is None:
            return self
        return instance.__dict__[self.name]

    def __set__(self, instance, value):
        if value <= 0:
            raise ValueError("must be positive")
        instance.__dict__[self.name] = value

class Product:
    price = Positive()
    def __init__(self, price):
        self.price = price
~~~

__getattr__ runs only after normal lookup fails. __getattribute__ intercepts every lookup and can recurse accidentally; use object.__getattribute__ carefully. Descriptors are powerful, but should solve a repeated design problem rather than add ceremony.

## 9. Dataclasses, enums, ABCs, and protocols

dataclasses.dataclass generates methods such as __init__ and __repr__. Use field(default_factory=list) for per-instance mutable defaults. frozen=True prevents ordinary field reassignment but does not deeply freeze nested objects. slots=True can reduce per-instance overhead.

Enum provides named symbolic values. Use IntEnum only when integer interoperability is intentional. Abstract base classes define explicit runtime interface requirements. Protocols support structural typing: a class can satisfy an interface by implementing required operations without inheriting from it. Use ABCs for shared implementation or runtime inheritance checks; use Protocols for flexible typed interfaces.

## 10. Advanced typing

Type hints document expected values and enable static analysis; Python generally does not enforce them at runtime. Learn built-in generics, unions, Literal, Final, ClassVar, TypedDict, Protocol, Callable, Iterable, Iterator, Sequence, Mapping, TypeVar, Generic, ParamSpec, Concatenate, overload, Self, TypeGuard, and TypeIs where supported by the target version.

~~~python
from typing import Protocol, TypeVar

class SupportsClose(Protocol):
    def close(self) -> None: ...

T = TypeVar("T")

def first(items: list[T]) -> T:
    if not items:
        raise ValueError("empty list")
    return items[0]
~~~

Use a checker such as mypy or Pyright. Type annotations are not input validation: validate untrusted values at runtime. State supported Python versions because typing features evolve.

## 11. Exception architecture

Catch the narrowest exception that can be handled. Avoid bare except in ordinary application logic. Preserve causes with raise NewError(...) from exc. Use custom exception classes for domain failures. Cleanup belongs in finally or context managers.

~~~python
class ConfigError(Exception):
    pass

def parse_port(raw):
    try:
        port = int(raw)
    except (TypeError, ValueError) as exc:
        raise ConfigError("port must be an integer") from exc
    if not 1 <= port <= 65535:
        raise ConfigError("port out of range")
    return port
~~~

Do not expose secrets in exceptions or logs. Translate low-level errors into stable domain-level errors at application boundaries while preserving diagnostic context.

## 12. Standard library fluency

Become comfortable with pathlib for paths; os, shutil, tempfile for filesystem work; argparse for CLIs; logging for diagnostics; json, csv, tomllib for data; datetime and zoneinfo for dates and time zones; collections for Counter, defaultdict, deque, and ChainMap; itertools and functools for functional tools; re for regular expressions; sqlite3 for embedded databases; decimal, fractions, statistics, and math for numerical work; unittest and unittest.mock for testing; subprocess for external programs; socket, ssl, and urllib for networking; concurrent.futures for pools; and asyncio for asynchronous I/O.

Always check exact behavior and availability against the documentation for the target Python version.

## 13. Filesystem and serialization boundaries

Use pathlib rather than manual path concatenation. Specify text encodings such as UTF-8. Stream large files line by line. Use temporary files and atomic replacement where robust writes are needed.

JSON is a data format, not a Python object serializer. Validate decoded data. Use csv rather than splitting rows on commas. Never unpickle untrusted data: pickle deserialization can execute arbitrary code. Avoid eval and exec on untrusted strings.

User-supplied paths are untrusted. Resolve a candidate path and verify it remains inside the permitted base directory before accessing it. Consider symlinks and platform-specific path rules. Merely joining a base path and user path is not sufficient protection.

## 14. Databases and transactions

Understand tables, primary and foreign keys, constraints, indexes, normalization, transactions, commit, rollback, and isolation. Always parameterize SQL values; never build SQL values through string interpolation.

~~~python
import sqlite3

with sqlite3.connect("app.db") as db:
    db.execute(
        "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY, body TEXT NOT NULL)"
    )
    db.execute("INSERT INTO notes(body) VALUES (?)", ("Study Python",))
~~~

Indexes can improve reads but consume storage and slow writes. Add them based on measured query patterns. Keep persistence behind a small data-access layer when that improves testing and maintainability.

## 15. Networking and APIs

Understand DNS, IP addresses, ports, TCP, TLS, HTTP methods, status codes, headers, content types, timeouts, and retries. Network calls can fail or stall: set timeouts. Retry only operations safe to retry, use bounded backoff, and respect rate limits. Validate response status and schema. Do not disable TLS verification in production. Store credentials outside source code and redact authorization headers from logs. Handle pagination, malformed payloads, and API version changes.

## 16. Threads and processes

Concurrency means tasks make progress during overlapping periods; parallelism means simultaneous execution. Threads share memory and are useful for I/O-bound work. Processes have separate memory and can use multiple CPU cores for CPU-bound work, with serialization and startup overhead.

The conventional CPython GIL limits simultaneous execution of Python bytecode by threads, but I/O can release it. Builds and Python versions may differ, including optional free-threaded builds. Do not assume threads are useless or that the GIL makes shared state safe. Protect shared mutable state with locks or prefer queues and message passing. Never assume a scheduling order.

## 17. Async Python

async def defines a coroutine function. Calling it creates a coroutine object; it does not run it to completion. await suspends the current coroutine while an awaitable makes progress. An event loop schedules tasks. Async is useful for many concurrent I/O operations, not automatically faster for CPU-heavy work.

~~~python
import asyncio

async def job(delay, label):
    await asyncio.sleep(delay)
    return label

async def main():
    results = await asyncio.gather(job(0.2, "A"), job(0.1, "B"))
    print(results)

asyncio.run(main())
~~~

Study tasks, task groups where available, cancellation, timeouts, async iterators, async generators, and async context managers. Cancellation is cooperative. Use try/finally to release resources and do not swallow cancellation accidentally. Avoid blocking calls in the event loop.

## 18. Testing strategy

Use unit tests for deterministic logic, integration tests for databases and service boundaries, end-to-end tests for important journeys, and regression tests for fixed defects. Test behavior rather than private implementation details. Mock only at boundaries where a real dependency is slow, costly, unavailable, or unreliable. Prefer dependency injection and temporary resources.

Test empty inputs, one item, boundaries, invalid types, Unicode, time zones, permission failures, timeouts, duplicate requests, and corrupted data. Passing tests increases confidence but does not prove there are no defects.

## 19. Debugging and observability

Reproduce the smallest failing case. Read the traceback, identify the exception and call path, then inspect state with a debugger, breakpoints, logging, or assertions. Change one thing at a time and add a regression test after fixing a defect.

Logs should explain what happened, when, and during which operation without leaking secrets. Use appropriate severity levels. Never log passwords, access tokens, private messages, or sensitive personal data.

## 20. Complexity and performance

Choose data structures based on operations. List membership is linear on average; set and dictionary lookup are typically constant-time on average; comparison sorting is generally O(n log n). Big-O describes growth, not exact runtime. Measure representative workloads with timeit and profiling tools.

Improve algorithms before micro-optimizing syntax. Avoid repeated work, unnecessary copies, and unbounded caches. Use generators for streams where appropriate. Caching requires invalidation and memory limits. Optimize only when measurement justifies the trade-off.

## 21. Packaging and environments

Use a virtual environment per project. Declare dependencies and supported Python versions in pyproject.toml. Understand source distributions, wheels, package metadata, entry points, and version constraints. Separate development dependencies from runtime dependencies. Test installation in a clean environment, not only from a source checkout.

A quality package has a clear import structure, public API, documentation, tests, and license. Avoid relying on the current working directory for imports or paths.

## 22. Security engineering

Treat external input as untrusted. Parameterize SQL. Avoid eval, exec, and unsafe deserialization. Pass argument lists to subprocess instead of shell command strings. Set network and subprocess timeouts. Validate paths against allowed roots. Keep secrets out of source, commits, logs, and exception text. Verify TLS certificates. Apply least privilege. Bound input sizes and resource use. Parsing is not validation.

~~~python
import subprocess

result = subprocess.run(
    ["python", "--version"],
    check=True,
    capture_output=True,
    text=True,
    timeout=5,
)
print(result.stdout.strip())
~~~

Never concatenate untrusted input into a shell command. Security also requires dependency updates, review, backups, and safe error handling.

## 23. Architecture and design patterns

Separate user interface, domain logic, persistence, networking, and configuration. Keep functions focused, name concepts clearly, and make dependencies explicit. Useful patterns include Strategy (interchangeable algorithms), Factory (object creation), Adapter (interface translation), Observer (event notification), Repository (persistence isolation), Command (operation as an object), and dependency injection (supply dependencies rather than constructing them everywhere).

Patterns are tools, not mandatory templates. Prefer the simplest design that meets requirements. Avoid premature abstraction and giant manager classes. Favor composition when it makes dependencies clearer.

## 24. CLI and automation engineering

A robust command-line tool validates arguments, prints useful help, returns meaningful exit codes, and sends diagnostics to stderr. Use argparse. Keep core functions independent from terminal input and output so they can be tested. Provide dry-run mode for destructive operations. Make file changes explicit and report failures clearly.

## 25. Numerical and scientific Python

The standard library provides math, statistics, decimal, and fractions. Scientific workloads often use third-party tools such as NumPy; these are ecosystem packages, not part of the Python language. Learn array shape, dtype, broadcasting, vectorization, precision, and memory layout before relying on optimized operations. Use Decimal for controlled base-10 arithmetic and Fraction for exact rational arithmetic when appropriate.

## 26. CPython internals and native extensions

Separate language behavior from CPython internals. CPython uses reference counting plus cyclic garbage collection, but application code should not depend on exact collection timing. weakref provides references that do not keep objects alive. Allocators and object layout are implementation details.

Native extensions can use the CPython C API or third-party tools such as pybind11. They introduce ABI, ownership, reference-counting, exception translation, and thread-state concerns. Justify native code with profiling and test across supported platforms. Embedding Python in Android is a deployment architecture decision, not a language feature.

## 27. Common advanced mistakes

1. Confusing identity with equality.
2. Using mutable defaults.
3. Sharing mutable thread state without synchronization.
4. Blocking an asyncio event loop.
5. Catching every exception and hiding its cause.
6. Assuming annotations validate runtime input.
7. Reusing an exhausted generator.
8. Forgetting decorators replace the decorated object.
9. Choosing inheritance where composition is simpler.
10. Building SQL or shell commands by concatenation.
11. Ignoring time zones and daylight-saving changes.
12. Optimizing without profiling.
13. Assuming the GIL makes code thread-safe.
14. Treating JSON parsing as schema validation.
15. Trusting a path simply because it was joined to a base directory.

## 28. Applied projects

Every project must include source, README, tests, error handling, and a short design explanation.

1. Log analyzer: stream large logs, count event types, summarize errors, and filter records.
2. Secure file organizer: preview changes, validate paths, avoid overwrites, and keep undo metadata.
3. SQLite notes CLI: CRUD, parameterized SQL, schema migration, and tests.
4. HTTP status checker: timeouts, bounded concurrency, retries, and structured output.
5. Async task monitor: cancellation, timeouts, and graceful shutdown.
6. Packaged utility: pyproject metadata, CLI entry point, typing, tests, and wheel build.
7. Mini web API: validation, persistence, authentication concepts, logging, and integration tests.
8. Plugin application: define a Protocol, discover plugins safely, and isolate plugin failures.
9. Data pipeline: stream input, validate records, transform data, and report invalid rows.
10. Native integration prototype: compare Python and native implementations only after profiling proves a bottleneck.

## 29. Mastery assessment — answers must be withheld until attempted

A learner is not advanced merely because they can repeat definitions. Require runnable code, reasoning, tests, and explanations.

### Concept questions

1. Explain name binding, aliasing, mutability, and identity with an example.
2. Explain LEGB and demonstrate nonlocal.
3. Explain why mutable defaults persist across calls.
4. Distinguish iterable, iterator, and generator.
5. Explain descriptor precedence and how property uses descriptors.
6. Explain MRO and cooperative multiple inheritance.
7. State the equality/hash contract.
8. Explain what type hints do and do not guarantee.
9. Compare threads, processes, and asyncio for I/O-bound and CPU-bound work.
10. Explain cancellation and cleanup in async code.
11. Explain transactions, parameterized SQL, and indexes.
12. Explain path traversal and how to constrain paths.
13. Explain why pickle and eval are unsafe with untrusted input.
14. Explain Big-O and why profiling matters.
15. Distinguish Python language guarantees from CPython details.

### Coding challenges

1. Implement an iterator that reads a sequence in fixed-size batches.
2. Write a decorator that records calls and preserves metadata.
3. Implement a descriptor validating a positive integer.
4. Write a context manager managing a temporary resource.
5. Implement a typed repository interface and SQLite implementation.
6. Write an async function running bounded concurrent jobs with timeouts.
7. Design a retry helper that avoids retrying non-retryable errors.
8. Build a CLI with subcommands, validation, and exit codes.
9. Test empty, invalid, boundary, Unicode, and failure cases.
10. Review code for SQL injection, path traversal, secret leakage, and unsafe subprocess use.

### Evaluation rubric

Correctness 30%; explanation and reasoning 20%; tests and edge cases 20%; safety and error handling 15%; readability and maintainability 10%; performance reasoning 5%.

Below 60%: revisit fundamentals. 60–74%: developing. 75–89%: competent. 90%+: strong, subject to unseen practical tasks. Never mark mastery from reading alone.

## 30. Learning and verification workflow

For each topic: read it, run examples in the target Python version, predict modified outputs before execution, solve exercises without copying, add failure tests, explain the design, and complete an unseen problem. Keep reference knowledge, practice examples, and assessment results separate. If the goal is neural learning, implement and run a real training/evaluation pipeline; retrieval records alone do not update neural weights.

## 31. Official references

- Python tutorial: https://docs.python.org/3/tutorial/
- Language reference: https://docs.python.org/3/reference/
- Standard library: https://docs.python.org/3/library/
- HOWTOs: https://docs.python.org/3/howto/
- Packaging guide: https://packaging.python.org/
- PEP index: https://peps.python.org/

Verify behavior against the documentation for the exact Python version being targeted.
