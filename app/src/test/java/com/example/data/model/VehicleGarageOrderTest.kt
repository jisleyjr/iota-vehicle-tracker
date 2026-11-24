package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleGarageOrderTest {

  @Test
  fun sortsByDisplayedYearMakeModelIgnoringCase() {
    val zebra = Vehicle(id = 1, make = "zebra", model = "Cargo", year = 2019)
    val subaru = Vehicle(id = 9, make = "Subaru", model = "Outback", year = 2018)
    val toyotaLower = Vehicle(id = 3, make = "toyota", model = "Camry", year = 2021)
    val toyotaAvalon = Vehicle(id = 2, make = "Toyota", model = "avalon", year = 2021)

    val ordered = listOf(toyotaLower, zebra, toyotaAvalon, subaru).sortedForGarage()

    assertEquals(
      listOf(
        "2018 Subaru Outback",
        "2019 zebra Cargo",
        "2021 Toyota avalon",
        "2021 toyota Camry"
      ),
      ordered.map { it.garageName() }
    )
  }

  @Test
  fun equalNamesKeepTheLowerIdFirst() {
    val later = Vehicle(id = 8, make = "Ford", model = "Focus", year = 2016)
    val earlier = Vehicle(id = 4, make = "ford", model = "Focus", year = 2016)

    val ordered = listOf(later, earlier).sortedForGarage()

    assertEquals(listOf(4, 8), ordered.map { it.id })
  }
}
