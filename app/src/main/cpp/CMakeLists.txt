cmake_minimum_required(VERSION 3.18)

project(fcitx5-android VERSION 0.0.4)

set(CMAKE_CXX_STANDARD 17)

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

set(CMAKE_INSTALL_PREFIX /usr)
# fcitx-utils/standardpath.cpp uses FCITX_INSTALL_LIBDATADIR,
# which is CMAKE_INSTALL_LIBDATADIR's absolute path
set(CMAKE_INSTALL_LIBDATADIR /usr/lib)
set(FCITX_INSTALL_PKGDATADIR /usr/share/fcitx5)
set(FCITX_INSTALL_LOCALEDIR /usr/share/locale)
set(LIBIME_INSTALL_PKGDATADIR table)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake" ${CMAKE_MODULE_PATH})
# extra-cmake-modules
if(DEFINED ENV{ECM_DIR})
    set(ECM_DIR $ENV{ECM_DIR})
else()
    set(ECM_DIR /usr/share/ECM/cmake)
endif()

set(PREBUILT_DIR "${CMAKE_CURRENT_SOURCE_DIR}/prebuilt")

# prebuilt fmt
set(fmt_DIR "${PREBUILT_DIR}/fmt/${ANDROID_ABI}/lib/cmake/fmt")
find_package(fmt)

# prebuilt libintl-lite
set(LibIntl_DIR "${PREBUILT_DIR}/libintl-lite/${ANDROID_ABI}/lib/cmake")
find_package(LibIntl)

# prebuilt libevent
set(Libevent_DIR "${PREBUILT_DIR}/libevent/${ANDROID_ABI}/lib/cmake/libevent")
find_package(Libevent)

option(ENABLE_TEST "" OFF)
option(ENABLE_COVERAGE "" OFF)
option(ENABLE_ENCHANT "" OFF)
option(ENABLE_X11 "" OFF)
option(ENABLE_WAYLAND "" OFF)
option(ENABLE_DBUS "" OFF)
option(ENABLE_DOC "" OFF)
option(ENABLE_SERVER "" OFF)
option(ENABLE_KEYBOARD "" OFF)
option(USE_SYSTEMD "" OFF)
option(ENABLE_XDGAUTOSTART "" OFF)
option(ENABLE_EMOJI "" OFF)
option(ENABLE_LIBUUID "" OFF)
add_subdirectory(fcitx5)

include(fcitx5/src/lib/fcitx-utils/Fcitx5Macros.cmake)
add_subdirectory(po)
add_subdirectory(androidfrontend)
add_subdirectory(androidkeyboard)

# prebuilt boost
set(BOOST_VERSION "1.80.0")
set(BOOST_MODULES headers filesystem atomic iostreams regex)
set(BOOST_ROOT "${PREBUILT_DIR}/boost/${ANDROID_ABI}")
set(Boost_DIR "${BOOST_ROOT}/lib/cmake/Boost-${BOOST_VERSION}")
foreach(mod IN LISTS BOOST_MODULES)
    set("boost_${mod}_DIR" "${BOOST_ROOT}/lib/cmake/boost_${mod}-${BOOST_VERSION}")
endforeach()

option(ENABLE_TEST "" OFF)
set(_Fcitx5Macro_SELF_DIR ${CMAKE_CURRENT_SOURCE_DIR}/fcitx5/src/lib/fcitx-utils)
# kenlm/util/exception.hh uses __FILE__ macro
add_compile_options(-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=.)
add_subdirectory(libime)

# prebuilt lua
include("${PREBUILT_DIR}/lua/${ANDROID_ABI}/lib/cmake/LuaConfig.cmake")

# we are using static linking
option(USE_DLOPEN "" OFF)
add_subdirectory(fcitx5-lua)

# prebuilt opencc
set(OpenCC_DIR "${PREBUILT_DIR}/opencc/${ANDROID_ABI}/lib/cmake/opencc")
find_package(OpenCC)

option(ENABLE_TEST "" OFF)
option(ENABLE_GUI "" OFF)
option(ENABLE_BROWSER "" OFF)
option(USE_WEBKIT "" OFF)
option(ENABLE_CLOUDPINYIN "" OFF)
# prefer OpenCC_DIR rather than fcitx5-chinese-addons/cmake/FindOpenCC.cmake
set(CMAKE_FIND_PACKAGE_PREFER_CONFIG ON)
add_subdirectory(fcitx5-chinese-addons)
# rename to include executable in apk
set_target_properties(scel2org5 PROPERTIES OUTPUT_NAME libscel2org5.so)

option(ENABLE_TEST "" OFF)
option(ENABLE_QT "" OFF)
add_subdirectory(fcitx5-unikey)
# suppress "illegal character encoding in character literal" warning in unikey/data.cpp
target_compile_options(unikey-lib PRIVATE "-Wno-invalid-source-encoding")

add_library(native-lib SHARED native-lib.cpp)
target_link_libraries(native-lib
        log
        libevent::core
        Fcitx5::Utils
        Fcitx5::Config
        Fcitx5::Core
        Fcitx5::Module::QuickPhrase
        Fcitx5::Module::Unicode
        Fcitx5::Module::Clipboard
        LibIME::Pinyin
        LibIME::Table)
