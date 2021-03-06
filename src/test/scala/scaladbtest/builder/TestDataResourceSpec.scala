package scaladbtest.builder

import scaladbtest.DataSourceSpecSupport
import scaladbtest.model.value.Value
import java.util.Date
import scaladbtest.model.{DefaultColumn, Column, TestData}
import javax.sql.DataSource

/*
* Copyright 2010 Ken Egervari
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

class TestDataResourceSpec extends DataSourceSpecSupport {

	val dslDir = resourceDir + "dsl/"
	var invalidDslDir = dslDir + "invalid/"

	var testData: TestData = _
	var testDataResource: TestDataResource = _

	override protected def initializeDataSourceReferences(dataSource: DataSource) {
		testData = new TestData(dataSource)
		testDataResource = new TestDataResource(testData)
	}

	describe("A Test Data Dsl") {

		describe("when has valid syntax") {
			it("should do nothing if file is empty") {
				testDataResource loadFrom (dslDir + "empty.dbt")

				testData.tables should have size (0)
			}

			it("should parse a single row with a single column") {
				testDataResource loadFrom (dslDir + "one_column.dbt")

				testData.tables should have size (1)
				testData.tables(0).name should equal ("user_account")

				testData.tables(0).records should have size (1)
				testData.tables(0).records(0).table.get should equal (testData.tables(0))
				testData.tables(0).records(0).label.get should equal ("ken")

				testData.tables(0).records(0).columns should have size (1)
				testData.tables(0).records(0).columns should contain (
					Column("first_name", Value.string("Ken"), Some(testData.tables(0).records(0)))
				)
			}

			it("should parse a value that contains many spaces") {
				testDataResource loadFrom (dslDir + "one_column_with_spaces.dbt")

				testData.tables should have size (1)
				testData.tables(0).name should equal ("user_account")

				testData.tables(0).records should have size (1)
				testData.tables(0).records(0).table.get should equal (testData.tables(0))
				testData.tables(0).records(0).label.get should equal ("ken")

				testData.tables(0).records(0).columns should have size (1)
				testData.tables(0).records(0).columns should contain (
					Column("full_name", Value.string("Ken Egervari"), Some(testData.tables(0).records(0))))
			}

			it("should parse $now for a column value and infer today's date") {
				testDataResource loadFrom (dslDir + "now_column.dbt")

				val formattedDate = Value.formatDate(new Date()).substring(0, 15)

				testData.tables(0).records(0).columns(0).name should equal ("date")
				testData.tables(0).records(0).columns(0).value.text.get should startWith (formattedDate)
			}

			it("should parse $null or null for a column value and infer None") {
				testDataResource loadFrom (dslDir + "null_column.dbt")

				testData.tables(0).records should have size (2)

				testData.tables(0).records(0).columns(0).name should equal ("col")
				testData.tables(0).records(0).columns(0).value.text should equal (None)

				testData.tables(0).records(1).columns(0).name should equal ("col")
				testData.tables(0).records(1).columns(0).value.text should equal (None)
			}

			it("should parse $true/$false and true/false for a column value and infer boolean") {
				testDataResource loadFrom (dslDir + "boolean_column.dbt")

				testData.tables(0).records should have size (4)

				testData.tables(0).records(0).columns(0).value.sqlValue should equal ("true")
				testData.tables(0).records(1).columns(0).value.sqlValue should equal ("true")
				testData.tables(0).records(2).columns(0).value.sqlValue should equal ("false")
				testData.tables(0).records(3).columns(0).value.sqlValue should equal ("false")
			}

			it("should parse $label and replace it with the label's name") {
				testDataResource loadFrom (dslDir + "label_column.dbt")

				testData.tables(0).records should have size (1)

				testData.tables(0).records(0).columns(0).name should equal ("col")
				testData.tables(0).records(0).columns(0).value.text.get should equal ("$label")
				testData.tables(0).records(0).columns(0).value.sqlValue should equal ("'record1'")
			}

			it("should parse a single row with 2 columns seperated by a comma") {
				testDataResource loadFrom (dslDir + "two_columns_with_comma.dbt")

				testData.tables should have size (1)
				testData.tables(0).name should equal ("user_account")

				testData.tables(0).records should have size (1)
				testData.tables(0).records(0).table.get should equal (testData.tables(0))
				testData.tables(0).records(0).label.get should equal ("ken")

				testData.tables(0).records(0).columns should have size (2)
				testData.tables(0).records(0).columns should contain (
					Column("first_name", Value.string("Ken"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("last_name", Value.string("Egervari"), Some(testData.tables(0).records(0))))
			}

			it("should parse two records from the same table") {
				testDataResource loadFrom (dslDir + "two_records.dbt")

				testData.tables should have size (1)
				testData.tables(0).name should equal ("user_account")

				testData.tables(0).records should have size (2)
				testData.tables(0).records(0).table.get should equal (testData.tables(0))
				testData.tables(0).records(0).label.get should equal ("ken")
				testData.tables(0).records(0).columns should have size (2)
				testData.tables(0).records(0).columns should contain (
					Column("first_name", Value.string("Ken"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("last_name", Value.string("Egervari"), Some(testData.tables(0).records(0))))

				testData.tables(0).records(1).table.get should equal (testData.tables(0))
				testData.tables(0).records(1).label.get should equal ("ben")
				testData.tables(0).records(1).columns should have size (2)
				testData.tables(0).records(1).columns should contain (
					Column("first_name", Value.string("Ben"), Some(testData.tables(0).records(1))))
				testData.tables(0).records(1).columns should contain (
					Column("last_name", Value.string("Sisko"), Some(testData.tables(0).records(1))))
			}

			it("should parse two anonyomous records (doesn't have labels)") {
				testDataResource loadFrom (dslDir + "anonymous_records.dbt")

				testData.tables should have size (1)
				testData.tables(0).name should equal ("country")

				testData.tables(0).records should have size (2)
				testData.tables(0).records(0).table.get should equal (testData.tables(0))
				testData.tables(0).records(0).label should equal (None)
				testData.tables(0).records(0).columns should have size (2)
				testData.tables(0).records(0).columns should contain (
					Column("id", Value.string("1"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("name", Value.string("Canada"), Some(testData.tables(0).records(0))))

				testData.tables(0).records(1).table.get should equal (testData.tables(0))
				testData.tables(0).records(1).label should equal (None)
				testData.tables(0).records(1).columns should have size (2)
				testData.tables(0).records(1).columns should contain (
					Column("id", Value.string("2"), Some(testData.tables(0).records(1))))
				testData.tables(0).records(1).columns should contain (
					Column("name", Value.string("United States"), Some(testData.tables(0).records(1))))
			}

			it("should parse 3 records from 2 different tables and maintain order they were written in") {
				testDataResource loadFrom (dslDir + "three_records_two_tables.dbt")

				testData.tables should have size (3)
				testData.tables(0).name should equal ("user_account")
				testData.tables(1).name should equal ("country")
				testData.tables(2).name should equal ("user_account")

				testData.tables(0).records should have size (1)
				testData.tables(0).records(0).table.get should equal (testData.tables(0))
				testData.tables(0).records(0).label.get should equal ("ken")
				testData.tables(0).records(0).columns should have size (2)
				testData.tables(0).records(0).columns should contain (
					Column("first_name", Value.string("Ken"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("last_name", Value.string("Egervari"), Some(testData.tables(0).records(0))))

				testData.tables(0).records should have size (1)
				testData.tables(1).records(0).table.get should equal (testData.tables(1))
				testData.tables(1).records(0).label.get should equal ("canada")
				testData.tables(1).records(0).columns should have size (1)
				testData.tables(1).records(0).columns should contain (
					Column("name", Value.string("Canada"), Some(testData.tables(1).records(0))))

				testData.tables(0).records should have size (1)
				testData.tables(2).records(0).table.get should equal (testData.tables(2))
				testData.tables(2).records(0).label.get should equal ("ben")
				testData.tables(2).records(0).columns should have size (2)
				testData.tables(2).records(0).columns should contain (
					Column("first_name", Value.string("Ben"), Some(testData.tables(2).records(0))))
				testData.tables(2).records(0).columns should contain (
					Column("last_name", Value.string("Sisko"), Some(testData.tables(2).records(0))))
			}

			it("should parse a file and skip any comments that begin with #") {
				testDataResource loadFrom (dslDir + "with_comments.dbt")

				testData.tables should have size (1)
				testData.tables(0).records should have size (1)
				testData.tables(0).records(0).columns should have size (2)
			}

			it("should parse table default values and populate the missing values in records") {
				testDataResource loadFrom (dslDir + "default_values.dbt")

				testData.tables should have size (1)
				testData.tables(0).name should equal ("province")
				testData.tables(0).defaultColumns should have size (2)
				testData.tables(0).defaultColumns should contain (
					DefaultColumn("country_id", Value.string("1"), Some(testData.tables(0))))
				testData.tables(0).defaultColumns should contain (
					DefaultColumn("nice_weather", Value.boolean(true), Some(testData.tables(0))))

				testData.tables(0).records should have size (3)

				testData.tables(0).records(0).columns should have size (4)
				testData.tables(0).records(0).columns should contain (
					Column("province_id", Value.string("1"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("name", Value.string("British Columbia"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("country_id", Value.string("1"), Some(testData.tables(0).records(0))))
				testData.tables(0).records(0).columns should contain (
					Column("nice_weather", Value.boolean(true), Some(testData.tables(0).records(0))))

				testData.tables(0).records(1).columns should have size (4)
				testData.tables(0).records(1).columns should contain (
					Column("province_id", Value.string("2"), Some(testData.tables(0).records(1))))
				testData.tables(0).records(1).columns should contain (
					Column("name", Value.string("Manitoba"), Some(testData.tables(0).records(1))))
				testData.tables(0).records(1).columns should contain (
					Column("country_id", Value.string("1"), Some(testData.tables(0).records(1))))
				testData.tables(0).records(1).columns should contain (
					Column("nice_weather", Value.boolean(false), Some(testData.tables(0).records(1))))

				testData.tables(0).records(2).columns should have size (4)
				testData.tables(0).records(2).columns should contain (
					Column("province_id", Value.string("3"), Some(testData.tables(0).records(2))))
				testData.tables(0).records(2).columns should contain (
					Column("name", Value.string("New York"), Some(testData.tables(0).records(2))))
				testData.tables(0).records(2).columns should contain (
					Column("country_id", Value.string("2"), Some(testData.tables(0).records(2))))
				testData.tables(0).records(2).columns should contain (
					Column("nice_weather", Value.boolean(true), Some(testData.tables(0).records(2))))
			}
		}

		describe("when has invalid syntax") {
			it("should throw exception if a comma exists at the end of a column list") {
				intercept[TestDataParseException] {
					testDataResource loadFrom (invalidDslDir + "extra_comma_at_end_of_columns.dbt")
				}
			}
		}
	}

}