## Process this file with automake to produce Makefile.in

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

lib_LTLIBRARIES = libcppc-c.la
if BUILD_CPPCF
  lib_LTLIBRARIES += libcppc-f.la
endif

if DEBUG
  AM_CXXFLAGS = -g -O0
endif


AM_CPPFLAGS = -I${srcdir}/include
libcppc_c_la_SOURCES = include/checkpointer/checkpoint.h checkpointer/checkpoint.cpp include/checkpointer/checkpointer.h checkpointer/checkpointer.cpp \
		     include/compressor/compressor.h compressor/compressor.cpp include/compressor/compressor_factory.h compressor/compressor_factory.cpp \
		     include/compressor/no_compressor.h compressor/no_compressor.cpp include/controller/controller.h controller/controller.cpp \
		     include/controller/actions/add_loop_index_action.h controller/actions/add_loop_index_action.cpp \
		     include/controller/actions/checkpoint_action.h controller/actions/checkpoint_action.cpp \
		     include/controller/actions/commit_call_image_action.h controller/actions/commit_call_image_action.cpp \
		     include/controller/actions/context_pop_action.h controller/actions/context_pop_action.cpp \
		     include/controller/actions/context_push_action.h controller/actions/context_push_action.cpp \
		     include/controller/actions/controller_helper.h controller/actions/controller_helper.cpp \
		     include/controller/actions/create_call_image_action.h controller/actions/create_call_image_action.cpp \
		     include/controller/actions/init_configuration_action.h controller/actions/init_configuration_action.cpp \
		     include/controller/actions/init_state_action.h controller/actions/init_state_action.cpp \
		     include/controller/actions/offset_set_action.h controller/actions/offset_set_action.cpp \
		     include/controller/actions/register_action.h controller/actions/register_action.cpp \
		     include/controller/actions/register_descriptor_action.h controller/actions/register_descriptor_action.cpp \
		     include/controller/actions/register_for_call_image_action.h controller/actions/register_for_call_image_action.cpp \
		     include/controller/actions/remove_loop_index_action.h controller/actions/remove_loop_index_action.cpp \
		     include/controller/actions/set_loop_index_action.h controller/actions/set_loop_index_action.cpp \
		     include/controller/actions/shutdown_action.h controller/actions/shutdown_action.cpp \
		     include/controller/actions/unregister_action.h controller/actions/unregister_action.cpp \
		     include/controller/actions/unregister_descriptor_action.h controller/actions/unregister_descriptor_action.cpp \
		     include/controller/actions/update_descriptor_action.h controller/actions/update_descriptor_action.cpp \
		     include/data/call_image.h data/call_image.cpp include/data/context.h data/context.cpp \
		     include/data/cppc_array.h data/cppc_array.cpp include/data/cppc_basic.h data/cppc_basic.cpp \
		     include/data/cppc_binary.h data/cppc_binary.cpp \
		     include/data/cppc_file.h data/cppc_file.cpp include/data/data_type.h data/data_type.cpp \
		     include/data/data_type_factory.h data/data_type_factory.cpp include/data/heap_context.h data/heap_context.cpp \
		     include/data/loop_context.h data/loop_context.cpp include/data/memory_block.h data/memory_block.cpp \
		     include/data/register.h data/register.cpp include/cppc.h interface/cppc.cpp \
		     include/util/singleton/singleton.h include/util/filemanager/file_system_manager.h \
		     include/util/filemanager/directory_stream.h include/util/communication/communication_manager.h \
		     include/util/configuration/configuration_manager.h util/configuration/configuration_manager.cpp \
		     include/util/xml/xml_parser.h util/xml/xml_parser.cpp \
		     include/util/xml/xml_parser_factory.h util/xml/xml_parser_factory.cpp include/writer/buffer.h writer/buffer.cpp \
		     include/writer/writer.h writer/writer.cpp include/writer/writer_factory.h writer/writer_factory.cpp
EXTRA_libcppc_c_la_SOURCES = include/compressor/zlib_compressor.h compressor/zlib_compressor.cpp \
			   include/util/communication/mpi_communicator.h util/communication/mpi_communicator.cpp \
			   include/util/communication/no_communicator.h util/communication/no_communicator.cpp \
			   include/util/filemanager/unix_filesystem_plugin.h util/filemanager/unix_filesystem_plugin.cpp \
			   include/util/xml/plain_parser.h util/xml/plain_parser.cpp \
			   include/util/xml/xerces_sax_parser.h util/xml/xerces_sax_parser.cpp \
			   include/writer/io_hdf5_writer.h writer/io_hdf5_writer.cpp
libcppc_c_la_LIBADD = @COMMUNICATOR_PLUGIN@ @FILEMANAGER_PLUGIN@ @XML_PARSER_PLUGIN@ @WRITER_PLUGINS@
libcppc_c_la_DEPENDENCIES = @COMMUNICATOR_PLUGIN@ @FILEMANAGER_PLUGIN@ @XML_PARSER_PLUGIN@ @WRITER_PLUGINS@
libcppc_c_la_LDFLAGS = -release @VERSION@

libcppc_f_la_SOURCES = interface/cppcf.cpp interface/fortran-interface.f
libcppc_f_la_LIBADD = libcppc-c.la
libcppc_f_la_LDFLAGS = -release @VERSION@

SUBDIRS = include
