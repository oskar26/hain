# Hain native layer (LLM.md §1, T-030). llama.cpp stays an untouched submodule.
cmake_minimum_required(VERSION 3.22.1)
project(hain_llm LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(HAIN_LLAMA_DIR ${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/llama.cpp)

# Only the core library: no tools, server, tests, curl/openssl, OpenMP.
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
# One CPU backend per ARM feature level (dotprod, i8mm, sve …), picked at runtime:
# no SIGILL on older phones, full speed on new ones. Needs shared libs.
set(GGML_BACKEND_DL ON CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS ON CACHE BOOL "" FORCE)

add_subdirectory(${HAIN_LLAMA_DIR} llama-build)

add_library(hain_llm SHARED hain_llm.cpp)
target_include_directories(hain_llm PRIVATE ${HAIN_LLAMA_DIR}/include ${HAIN_LLAMA_DIR}/ggml/include)
target_link_libraries(hain_llm PRIVATE llama ggml android log)
target_link_options(hain_llm PRIVATE "-Wl,-z,max-page-size=16384")
