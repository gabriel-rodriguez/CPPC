//    This file is part of CPPC.
//
//    CPPC is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    CPPC is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with CPPC; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA




#if ! defined CPPC_UTIL_COMMUNICATION_COMMUNICATIONMANAGER_H
#define CPPC_UTIL_COMMUNICATION_COMMUNICATIONMANAGER_H

#include <data/data_type.h>

using cppc::data::DataType;

namespace cppc {
  namespace util {
    namespace communication {

      class CPPC_COMMUNICATION_PLUGIN {
      public:
	static void barrier();
	static int getRank();
	static int getWorldSize();
	static DataType * reductionOnMax( DataType * );
	static DataType * reductionOnMin( DataType * );
      };

      template <typename CommunicationPlugin> class InternalCommunicationManager {

      public:
	typedef int RankType;

	static inline void barrier() {
	  CommunicationPlugin::barrier();
	}

	static inline RankType getRank() {
	  return CommunicationPlugin::getRank();
	}

	static inline int getWorldSize() {
	  return CommunicationPlugin::getWorldSize();
	}

	static inline DataType * reductionOnMax( DataType * data ) {
	  return CommunicationPlugin::reductionOnMax( data );
	}

	static inline DataType * reductionOnMin( DataType * data ) {
	  return CommunicationPlugin::reductionOnMin( data );
	}

      private:
	InternalCommunicationManager() {}
	InternalCommunicationManager( const InternalCommunicationManager & ) {}
	InternalCommunicationManager & operator=( const InternalCommunicationManager & ) { return *this; }
	~InternalCommunicationManager() {}

      };

      typedef InternalCommunicationManager<CPPC_COMMUNICATION_PLUGIN> CommunicationManager;

    }
  }
}

#endif
