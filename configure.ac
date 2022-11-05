dnl Process this file with autoconf to produce a configure script

AC_INIT([cppc], [0.8.2], [grodriguez@udc.es])
m4_include([m4/lx_find_mpi.m4])
m4_include([m4/ax_lib_hdf5.m4])
m4_include([m4/ax_compare_version.m4])
m4_include([m4/ax_pthread.m4])
m4_include([m4/ax_lib_xerces.m4])
AC_CONFIG_HEADERS(src/include/autoconf.h)
AC_CONFIG_SRCDIR([src/include/cppc.h.in])
AC_CONFIG_AUX_DIR([config])
AM_INIT_AUTOMAKE([-Wall -Werror foreign])
AM_PROG_AR

AC_PROG_CXX
AC_LANG_CPLUSPLUS

dnl Search for the C compiler and preprocessor
AC_PROG_CC
AM_PROG_CC_C_O
AC_PROG_CPP

dnl If F77 does not support FSEEK, do not compile Fortran library (or if it is disabled)
build_cppcf="yes"
AC_ARG_ENABLE( fortran,
               [AC_HELP_STRING( [--disable-fortran],
                                [disable Fortran 77 support [enabled]])],
               build_cppcf="${enableval}" )

if test "x${build_cppcf}" = "xyes";
then
  dnl Search for the Fortran compiler
  AC_PROG_F77
  
  if test "x${F77}" != "x";
  then
#    AC_LANG_SAVE
#    AC_LANG([Fortran 77])
	AC_LANG_PUSH([Fortran 77])
    AC_LINK_IFELSE( AC_LANG_PROGRAM(,[      CALL FSEEK(0,0,0)]), 
                    build_cppcf="yes", 
                    build_cppcf="no" )
#    AC_LANG_RESTORE
	AC_LANG_POP([Fortran 77])

    dnl Define F77_FUNC for consistently communicating Fortran and C
    AC_F77_WRAPPERS
    found_f77="${F77}"
  else
  	found_f77="not found"
    build_cppcf="no"
  fi
fi

AM_CONDITIONAL( [BUILD_CPPCF], [test "x${build_cppcf}" = "xyes"] )

dnl Search for libtool
AC_DISABLE_STATIC
AC_PROG_LIBTOOL

dnl Require OpenSSL
AC_CHECK_LIB( [crypto], [OPENSSL_config],, [AC_MSG_ERROR[libcrypto is required in order to build CPPC v0.8]] )
AC_CHECK_LIB( [ssl], [SHA1],,[AC_MSG_ERROR([openssl is required in order to build CPPC v0.8])], [-lcrypto] )

dnl ------------------------------------------------------------ COMM PLUGIN
dnl Check whether the user wants MPI (enable by default)
build_mpi="auto"
AC_ARG_WITH( mpi,
			 [AC_HELP_STRING([--with-mpi=yes/no/PATH], [location of mpicc for MPI configuration])],
			 [case "${withval}" in
			    no) build_mpi="no";;
			    yes) build_mpi="yes";; 
			    *) if test -x "${withval}";
			       then
			         MPICC="${withval}"
			         build_mpi="yes"
			       else
			         AC_MSG_ERROR(["--with-mpi=${withval}: file does not exist or is not executable"])
			       fi;;
			  esac])

if test "x${build_mpi}" != "xno";
then
  AC_LANG_PUSH([C])
  LX_FIND_MPI
  AC_LANG_POP([C])
  
  if test "x${build_mpi}" = "xyes";
  then
    if test "x${have_C_mpi}" != "xyes";
    then
      AC_MSG_ERROR(["--with-mpi used but no suitable MPI Compiler found."])
    fi
  fi
  
  if test "x${have_C_mpi}" != "xyes";
  then
    build_mpi="no"
  else
    build_mpi="yes"
    CXXFLAGS="${MPI_CFLAGS} ${CXXFLAGS}"
  fi 
fi

dnl set mpi-related variables and conditionals
if test "x${build_mpi}" = "xyes";
then
  DETECTED_COMMUNICATOR=MpiCommunicator
  COMMUNICATOR_PLUGIN=util/communication/mpi_communicator.lo
fi
AM_CONDITIONAL( [BUILD_MPI], [test "x${build_mpi}" = "xyes"] )

dnl If no communication plugin was found, fallback to NoCommunicator
if test -z "${DETECTED_COMMUNICATOR}"
then
  DETECTED_COMMUNICATOR="NoCommunicator"
  COMMUNICATOR_PLUGIN=util/communication/no_communicator.lo
fi

dnl In any case, add the def flag to CPPFLAGS
CPPFLAGS="${CPPFLAGS} -DCPPC_COMMUNICATION_PLUGIN=${DETECTED_COMMUNICATOR}"

dnl ------------------------------------------------------------ WRITER PLUGINS
WRITER_PLUGINS=""

dnl Check for HDF-5
AX_LIB_HDF5([serial])
if test "x${with_hdf5}" = "xyes";
then
  AX_COMPARE_VERSION( [${HDF5_VERSION}], [lt], [1.8], [AC_MSG_ERROR([HDF5 should be at least v1.8 (detected ${HDF5_VERSION})])] ) 
  WRITER_PLUGINS="writer/io_hdf5_writer.lo ${WRITER_PLUGINS}";
  CXXFLAGS="${HDF5_CPPFLAGS} ${CXXFLAGS}";
  LDFLAGS="${HDF5_LDFLAGS} ${LDFLAGS}";
  wplugins_message="|HDF5 Writer| ${wplugins_message}";
fi

if test -z "${WRITER_PLUGINS}";
then
  wplugins_message="WARNING -- No writing plugins being created (checkpoint creation impossible)"
fi

dnl Initialize the XML_PARSER_PLUGIN variable to the default option
XML_PARSER_PLUGIN="util/xml/plain_parser.lo"
AX_LIB_XERCES
if test "x${xerces_requested}" = "xyes"
then
  if test "x${HAVE_XERCES}" = "xyes";
  then
    CXXFLAGS="${XERCES_CFLAGS} ${CXXFLAGS}";
    LDFLAGS="${XERCES_LDFLAGS} ${LDFLAGS}";
    CONF_FILE_FORMAT="XML";
  else
    AC_MSG_ERROR( ["Stopping"] )
  fi
else
  CONF_FILE_FORMAT="Plain"
fi

dnl Check for a compatible filesystem (Unix is the only one supported right now)
AC_CHECK_HEADER( unistd.h,
  FILEMANAGER_PLUGIN=util/filemanager/unix_filesystem_plugin.lo
  DESCRIPTOR_TYPES_INCLUDE_FILE="util/filemanager/unix_descriptor_types.h"
  CPPFLAGS="${CPPFLAGS} -DCPPC_FILESYSTEM_PLUGIN=UnixFileSystemPlugin")

if test -z "${FILEMANAGER_PLUGIN}"
then
  AC_MSG_ERROR( [No compatible filesystem plugin found] )
fi

dnl Checking for -lpthread
AC_CHECK_LIB( [pthread], [pthread_create], [],
  [AC_MSG_ERROR( -lpthread is needed to compile CPPC. Please install the pthread libraries )] )

dnl If there is no installation directory selected, choose one
if test $prefix = "NONE"
then
  prefix=${PWD}/Build
  AC_MSG_WARN( Prefix not selected: using ${prefix} as default installation directory )
  prefix_message="${prefix} (override with --prefix)"
else
  prefix_message="${prefix}"
fi

dnl Debug build
AC_ARG_ENABLE( [debug], 
			   [AC_HELP_STRING([--enable-debug], [enable debugging, default:no])], 
			   [case "${enableval}" in
			     yes) debug=true;;
			     no)  debug=false;;
			     *) AC_MSG_ERROR([bad value ${enableval} for --enable-debug]);;
			   esac], 
			   [debug=false] )
AM_CONDITIONAL( [DEBUG], [test "x${debug}" = "xtrue"] ) 

CXXFLAGS="${CXXFLAGS} -Wall -pedantic -Wno-long-long"

AC_SUBST(COMMUNICATOR_PLUGIN)
AC_SUBST(DESCRIPTOR_TYPES_INCLUDE_FILE)
AC_SUBST(FILEMANAGER_PLUGIN)
AC_SUBST(MPI_CFLAGS)
AC_SUBST(MPI_CLDFLAGS)
AC_SUBST(prefix)
AC_SUBST(WRITER_PLUGINS)
AC_SUBST(XML_PARSER_PLUGIN)

AC_CONFIG_FILES([Makefile
	   Example/src/gauss-semantic.xml
	   Example/src/gauss-mpi-semantic.xml
	   Example/Makefile
	   Example/matgen/Makefile
	   bin/cppc_cc
	   bin/cppc_fc
	   bin/Makefile
	   compiler/Makefile
	   src/include/cppc.h
	   src/Makefile
	   src/include/Makefile])
AC_OUTPUT

chmod 700 bin/cppc_cc
chmod 700 bin/cppc_fc

cat << EOF

Config files successfully generated!

  Install prefix: ${prefix_message}

  C++ compiler: ${CXX}
  Fortran compiler: ${found_f77}

  Building Fortran interface: ${build_cppcf}
  Building MPI support: ${build_mpi}
  Building writers: ${wplugins_message}
  Configuration file format: ${CONF_FILE_FORMAT}

EOF
