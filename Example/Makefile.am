#    This file is part of CPPC.
#
#    CPPC is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    CPPC is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with CPPC; if not, write to the Free Software
#    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

AUTOMAKE_OPTIONS = subdir-objects

all: sysgen

SYSTAM=1000
SUBDIRS = matgen

if BUILD_MPI
  noinst_PROGRAMS = gauss-mpi
else
  noinst_PROGRAMS = gauss
endif

gauss_SOURCES = cppc_output/gauss.c
gauss_LDFLAGS = -L${libdir} -lcppc-c
gauss_CFLAGS = -I@prefix@/include
gauss_mpi_SOURCES = cppc_output/gauss-mpi.c
gauss_mpi_LDFLAGS = -L${libdir} -lcppc-c @MPI_CLDFLAGS@
gauss_mpi_CFLAGS = -I@prefix@/include @MPI_CFLAGS@

cppc_output/gauss.c: src/gauss.c src/gauss-semantic.xml
	${bindir}/cppc_cc -g src/gauss-semantic.xml -nocomms src/gauss.c

cppc_output/gauss-mpi.c: src/gauss-mpi.c src/gauss-mpi-semantic.xml
	${bindir}/cppc_cc -g src/gauss-mpi-semantic.xml src/gauss-mpi.c

sysgen: matgen/matgen
	matgen/matgen $(SYSTAM) $(SYSTAM) A
	matgen/matgen 1 $(SYSTAM) b
