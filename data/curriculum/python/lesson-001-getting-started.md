# Python Lesson 001 — Getting started, syntax and output

**Course:** Dev Orus 1.1 — Complete Python Course  
**Scope:** Python only  
**Level:** Beginner  
**Status:** Written learning material; not evidence of neural-weight training.

## Learning objectives

By the end of this lesson, the learner should be able to:
1. Explain what Python and the Python interpreter are.
2. Distinguish interactive mode (REPL) from running a saved script.
3. Write and run a small Python program.
4. Use `print()`, comments and basic string literals.
5. Explain why indentation matters.
6. Recognize and begin debugging a simple syntax error.

## 1. What is Python?

Python is a general-purpose, high-level programming language. It is used for automation, command-line tools, web services, data processing, scientific work, testing and many other kinds of software.

A Python source file commonly ends in `.py`. The Python interpreter executes Python programs. The interpreter is a program installed on the computer; it is not the same thing as the source code.

## 2. Interactive mode and script mode

- **Interactive mode (REPL):** enter an expression and see its result immediately. Start it with `python` or `python3`, depending on the installation.
- **Script mode:** save code in a file such as `hello.py`, then run `python hello.py` or `python3 hello.py`.

A script is useful for programs that should be saved, repeated, tested and shared.

## 3. First program

```python
print("Hello, world!")
```

Output:

```text
Hello, world!
```

`print()` is a built-in function. It writes the text representation of its arguments to standard output. Text is represented by a string, commonly written inside single or double quotation marks.

These are both valid:

```python
print('Hello')
print("Hello")
```

Quotes must match. A missing closing quote is a syntax error.

## 4. Printing more than one value

```python
print("Dev", "Orus", 1.1)
```

Output:

```text
Dev Orus 1.1
```

By default, `print()` separates multiple arguments with a space and ends with a newline. Its `sep` and `end` parameters can change those defaults:

```python
print("red", "green", "blue", sep=", ")
print("Ready", end=" ")
print("to learn")
```

Output:

```text
red, green, blue
Ready to learn
```

## 5. Comments

A comment begins with `#` outside a string. Python ignores the comment as code.

```python
# This line explains the next instruction.
print("Learning Python")
```

Output:

```text
Learning Python
```

Write comments to explain intent or a non-obvious decision. Do not add comments that merely repeat every obvious line.

## 6. Indentation and code blocks

Python uses indentation to group statements into blocks. Four spaces per indentation level is the common convention.

```python
if True:
    print("This line belongs to the if block")

print("This line is outside the block")
```

Output:

```text
This line belongs to the if block
This line is outside the block
```

The colon introduces the block. The indented line belongs to it. Inconsistent indentation can cause an `IndentationError` or make the program's structure different from what was intended. Do not mix tabs and spaces.

## 7. Statements and expressions

An **expression** produces a value, such as `2 + 3` or `"Py" + "thon"`. A **statement** performs an action, such as calling a function or assigning a name.

```python
2 + 3
print(2 + 3)
```

In a REPL, the first expression's value is usually displayed automatically. In a script, an expression by itself does not normally display its value; use `print()` when output is required.

Output from the second line:

```text
5
```

## 8. Common beginner errors

| Error | Typical cause | First check |
|---|---|---|
| `SyntaxError` | Invalid Python grammar, missing colon or quote | Read the reported line and nearby lines |
| `IndentationError` | A block is not indented consistently | Check the colon and spaces |
| `NameError` | A name is used before it is defined or is misspelled | Check spelling and execution order |
| No visible output | A script calculates a value but never prints it | Add an intentional `print()` |

Error messages are clues. Read the exception type and line number, then inspect the relevant code.

## 9. Worked example

```python
# A tiny introduction program
print("Welcome to Dev Orus 1.1")
print("Python course: lesson one")
print("2 + 3 =", 2 + 3)
```

Output:

```text
Welcome to Dev Orus 1.1
Python course: lesson one
2 + 3 = 5
```

## 10. Practice questions

1. What is the difference between the Python interpreter and a Python source file?
2. What is the usual file extension for a Python source file?
3. What does `print()` do?
4. What character starts a single-line comment?
5. Why is indentation important in Python?
6. Predict the output:

```python
print("A", "B", sep="-")
print("C", end="!")
print("D")
```

7. Find and fix the syntax problem:

```python
if True
    print("Ready")
```

8. Write a three-line program that prints your project name, its version and the text `Python begins here`.

## 11. Answer key

1. The interpreter executes Python code; the source file contains the code written by the programmer.
2. `.py`.
3. It writes values to standard output.
4. `#`.
5. Indentation defines the grouping of statements in blocks.
6. Output is `A-B` then `C!D` on the next line.
7. Add a colon after the condition: `if True:`.
8. One possible answer:

```python
print("Dev Orus")
print("Version 1.1")
print("Python begins here")
```

## 12. Mastery check

A correct response should demonstrate all of the following:
- Explain interpreter versus source file without confusing them.
- Write a valid script with correct indentation.
- Predict the output of the separator/end example.
- Correct the missing-colon error.
- Explain why a bare expression in a script does not automatically print its value.

Do not mark this lesson mastered until the learner can solve a fresh equivalent problem without copying the answer key.

---

**Next lesson:** Python names, variables, assignment and naming conventions.
