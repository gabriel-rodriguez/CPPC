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




#include <data/cppc_basic.h>
#include <data/data_type_factory.h>
#include <writer/writer.h>

namespace {

  using cppc::data::CharacterType;
  using cppc::data::DataType;
  using cppc::data::DataTypeFactory;
  using cppc::data::DoubleType;
  using cppc::data::FloatType;
  using cppc::data::IntegerType;
  using cppc::data::LongType;
  using cppc::data::ShortType;
  using cppc::data::UCharacterType;
  using cppc::data::UIntegerType;
  using cppc::data::ULongType;
  using cppc::data::UShortType;

  template <class T> DataType * createDataType() {
    return new T;
  }

  const bool registerCharacter = DataTypeFactory::instance().registerDataType( CharacterType::typeIdentifier, CharacterType::staticSize(), createDataType<CharacterType> );
  const bool registerDouble = DataTypeFactory::instance().registerDataType( DoubleType::typeIdentifier, DoubleType::staticSize(), createDataType<DoubleType> );
  const bool registerFloat = DataTypeFactory::instance().registerDataType( FloatType::typeIdentifier, FloatType::staticSize(), createDataType<FloatType> );
  const bool registerInteger = DataTypeFactory::instance().registerDataType( IntegerType::typeIdentifier, IntegerType::staticSize(), createDataType<IntegerType> );
  const bool registerLong = DataTypeFactory::instance().registerDataType( LongType::typeIdentifier, LongType::staticSize(), createDataType<LongType> );
  const bool registerShort = DataTypeFactory::instance().registerDataType( ShortType::typeIdentifier, ShortType::staticSize(), createDataType<ShortType> );
  const bool registerUCharacter = DataTypeFactory::instance().registerDataType( UCharacterType::typeIdentifier, UCharacterType::staticSize(), createDataType<UCharacterType> );
  const bool registerUInteger = DataTypeFactory::instance().registerDataType( UIntegerType::typeIdentifier, UIntegerType::staticSize(), createDataType<UIntegerType> );
  const bool registerULong = DataTypeFactory::instance().registerDataType( ULongType::typeIdentifier, ULongType::staticSize(), createDataType<ULongType> );
  const bool registerUShort = DataTypeFactory::instance().registerDataType( UShortType::typeIdentifier, UShortType::staticSize(), createDataType<UShortType> );

}
