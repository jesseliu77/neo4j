/**
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_3.ast.rewriters

import org.neo4j.cypher.internal.commons.CypherFunSuite
import org.neo4j.cypher.internal.compiler.v2_3.inSequence

class IsolateAggregationTest extends CypherFunSuite with RewriteTest {
  val rewriterUnderTest = isolateAggregation

  test("does not rewrite things that should not be rewritten") {
    assertIsNotRewritten("MATCH n RETURN n AS n")
    assertIsNotRewritten("MATCH n RETURN n AS n, count(*) AS count, max(n.prop) AS max")
  }

  test("MATCH (n) RETURN { name: n.name, count: count(*) } AS result") {
    assertRewrite(
      "MATCH (n) RETURN { name: n.name, count: count(*) } AS result",
      "MATCH (n) WITH n.name AS `  AGGREGATION27`, count(*) AS `  AGGREGATION40` RETURN { name: `  AGGREGATION27`, count: `  AGGREGATION40` } AS result")
  }

  test("MATCH n RETURN n.foo + count(*) AS result") {
    assertRewrite(
      "MATCH n RETURN n.foo + count(*) AS result",
      "MATCH n WITH n.foo AS `  AGGREGATION17`, count(*) AS `  AGGREGATION23` RETURN `  AGGREGATION17` + `  AGGREGATION23` AS result")
  }

  test("MATCH n RETURN count(*)/60/42 AS result") {
    assertRewrite(
      "MATCH n RETURN count(*)/60/42 AS result",
      "MATCH n WITH count(*) AS `  AGGREGATION15` RETURN `  AGGREGATION15`/60/42 AS result")
  }

  test("MATCH n-->() RETURN (n)-->({k: count(*)}) AS result") {
    assertRewrite(
      "MATCH n-->() RETURN (n)-->({k: count(*)}) AS result",
      "MATCH n-->() WITH n, count(*) AS `  AGGREGATION31` RETURN (n)-->({k:`  AGGREGATION31`}) AS result")
  }

  test("MATCH n RETURN n.prop AS prop, n.foo + count(*) AS count") {
    assertRewrite(
      "MATCH n RETURN n.prop AS prop, n.foo + count(*) AS count",
      "MATCH n WITH n.prop AS `  AGGREGATION17`, n.foo AS `  AGGREGATION33`, count(*) AS `  AGGREGATION39` RETURN `  AGGREGATION17` AS prop, `  AGGREGATION33` + `  AGGREGATION39` AS count")
  }

  test("MATCH n RETURN n AS n, count(n) + 3 AS count") {
    assertRewrite(
      "MATCH n RETURN n AS n, count(n) + 3 AS count",
      "MATCH n WITH n AS n, count(n) as `  AGGREGATION23`  RETURN n AS n, `  AGGREGATION23` + 3 AS count")
  }

  test("UNWIND [1,2,3] AS a RETURN reduce(y=0, x IN collect(a) | x) AS z") {
    assertRewrite(
      "UNWIND [1,2,3] AS a RETURN reduce(y=0, x IN collect(a) | x) AS z",
      "UNWIND [1,2,3] AS a WITH collect(a) AS `  AGGREGATION44` RETURN reduce(y=0, x IN `  AGGREGATION44` | x) AS z")
  }

  test("UNWIND [1,2,3] AS a RETURN filter(x IN collect(a) WHERE x <> 0) AS z") {
    assertRewrite(
      "UNWIND [1,2,3] AS a RETURN filter(x IN collect(a) WHERE x <> 0) AS z",
      "UNWIND [1,2,3] AS a WITH collect(a) AS `  AGGREGATION39` RETURN filter(x IN `  AGGREGATION39` WHERE x <> 0) AS z")
  }

  test("UNWIND [1,2,3] AS a RETURN extract(x IN collect(a) | x) AS z") {
    assertRewrite(
      "UNWIND [1,2,3] AS a RETURN extract(x IN collect(a) | x) AS z",
      "UNWIND [1,2,3] AS a WITH collect(a) AS `  AGGREGATION40` RETURN extract(x IN `  AGGREGATION40` | x) AS z")
  }

  test("UNWIND [1,2,3] AS a RETURN [x IN collect(a) | x] AS z") {
    assertRewrite(
      "UNWIND [1,2,3] AS a RETURN [x IN collect(a) | x] AS z",
      "UNWIND [1,2,3] AS a WITH collect(a) AS `  AGGREGATION33` RETURN [x IN `  AGGREGATION33` | x] AS z")
  }


  test("MATCH n WITH 60/60/count(*) AS x RETURN x AS x") {
    assertRewrite(
      "MATCH n WITH 60/60/count(*) AS x RETURN x AS x",
      "MATCH n WITH 60/60 AS `  AGGREGATION15`, count(*) AS `  AGGREGATION19` WITH `  AGGREGATION15`/`  AGGREGATION19` AS x RETURN x AS x")
  }

  test("MATCH (a:Start)<-[:R]-(b) RETURN { foo:a.prop=42, bar:collect(b.prop2) } AS result") {
    assertRewrite(
      "MATCH (a:Start)<-[:R]-(b) " +
      "RETURN { foo:a.prop=42, bar:collect(b.prop2) } AS result",

      "MATCH (a:Start)<-[:R]-(b) " +
      "WITH a.prop=42 AS `  AGGREGATION45`, collect(b.prop2) AS `  AGGREGATION54` " +
      "RETURN { foo:`  AGGREGATION45`, bar:`  AGGREGATION54`} AS result")
  }

  test("MATCH n RETURN count(*) + max(id(n)) AS r") {
    assertRewrite(
      "MATCH n RETURN count(*) + max(id(n)) AS r",
      "MATCH n WITH count(*) AS `  AGGREGATION15`, max(id(n)) AS `  AGGREGATION26` RETURN `  AGGREGATION15`+`  AGGREGATION26` AS r")
  }

  test("MATCH a RETURN length(collect(a)) AS length") {
    assertRewrite(
      "MATCH a RETURN length(collect(a)) AS length",
      "MATCH a WITH collect(a) AS `  AGGREGATION22` RETURN length(`  AGGREGATION22`) AS length")
  }

  test("MATCH a RETURN count(a) > 0 AS bool") {
    assertRewrite(
      "MATCH a RETURN count(a) > 0 AS bool",
      "MATCH a WITH count(a) AS `  AGGREGATION15` RETURN `  AGGREGATION15` > 0 AS bool")
  }

  test("MATCH a RETURN count(a) > {param} AS bool") {
    assertRewrite(
      "MATCH a RETURN count(a) > {param} AS bool",
      "MATCH a WITH count(a) AS `  AGGREGATION15` RETURN `  AGGREGATION15` > {param} AS bool")
  }

  override protected def parseForRewriting(queryText: String) =
    super.parseForRewriting(queryText).endoRewrite(inSequence(normalizeReturnClauses, normalizeWithClauses))
}
