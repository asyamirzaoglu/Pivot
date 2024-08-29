package com.example.pivot.functions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.reflect.full.memberProperties

// Data class representing sales data
data class SalesData(
    val productName: String,
    val category: String,
    val salesQuantity: Int,
    val salesAmount: Double,
    val quality: String,
    val tax: Int,
    val classification: String
)

// Main Composable function for the Pivot Table
@Composable
fun Pivot() {
    // Sample data for demonstration
    val sampleData = remember {
        listOf(
            SalesData("Product A", "Electronics", 100, 2500.0, "A", 18, "class1"),
            SalesData("Product B", "Electronics", 150, 3750.0, "B", 8, "class2"),
            SalesData("Product C", "Furniture", 80, 2400.0, "C", 18, "class2"),
            SalesData("Product D", "Furniture", 60, 1800.0, "A", 8, "class3"),
            SalesData("Product E", "Clothing", 200, 5000.0, "B", 10, "class1"),
            SalesData("Product F", "Clothing", 100, 3000.0, "C", 5, "class3"),
            SalesData("Product Z", "Furniture", 70, 1700.0, "A", 10, "class1")
        )
    }

    // Define filter state
    val applyFilters = false
    val filters = mutableMapOf<String, List<Any>>()

    if (applyFilters) {
        filters["productName"] = listOf("Product A", "Product C")
        filters["salesAmount"] = listOf(2500.0, 2400.0)
        filters["tax"] = listOf(18, 8)
        filters["classification"] = listOf("class1", "class2")
    }

    // Apply filters to sample data
    val filteredData = if (filters.isEmpty()) {
        sampleData
    } else {
        sampleData.filter { dataItem ->
            filters.all { (field, values) ->
                when (val propertyValue = getPropertyValue<SalesData>(dataItem, field)) {
                    is String -> values.any { it.toString() == propertyValue }
                    is Int -> values.any { it.toString().toIntOrNull() == propertyValue }
                    is Double -> values.any { it.toString().toDoubleOrNull() == propertyValue }
                    else -> false
                }
            }
        }
    }

    // Define header names
    val columnHeaders = listOf("category", "productName", "quality")
    val rowHeaders = listOf("classification", "tax", "salesQuantity")
    val valueField = "salesAmount"

    // Generate header values
    val columnHeaderValues = columnHeaders.take(1).flatMap { header ->
        filteredData.map { getPropertyValue<SalesData>(it, header).toString() }.distinct()
    }
    val nestedHeaderValuesByColumn = columnHeaderValues.associateWith { columnValue ->
        filteredData.filter { getPropertyValue<SalesData>(it, columnHeaders[0]) == columnValue }
            .map { columnHeaders.getOrNull(1)
                ?.let { it1 -> getPropertyValue<SalesData>(it, it1).toString() } }.distinct()
    }

    val thirdHeaderValuesByNestedHeader = filteredData.groupBy {
        Pair(
            columnHeaders.getOrNull(0)?.let { it1 -> getPropertyValue<SalesData>(it, it1) },
            columnHeaders.getOrNull(1)?.let { it1 -> getPropertyValue<SalesData>(it, it1) }
        )
    }.mapValues { (_, items) ->
        items.map { columnHeaders.getOrNull(2)
            ?.let { it1 -> getPropertyValue<SalesData>(it, it1).toString() } }.distinct()
    }

    // Layout for the pivot table
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Dynamic Pivot Table",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                // Render column headers
                Row(Modifier.fillMaxWidth()) {
                    TableCell(text = "", width = 120.dp) // Top-left corner cell

                    columnHeaderValues.forEach { category ->
                        val headerWidth =
                            nestedHeaderValuesByColumn[category]?.sumOf { productName ->
                                (thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.size
                                    ?: 0) * 120
                            }?.dp ?: 0.dp
                        TableCell(text = category, width = headerWidth)
                    }
                }

                // Render second header row if applicable
                if (columnHeaders.size > 1) {
                    Row(Modifier.fillMaxWidth()) {
                        TableCell(text = "", width = 120.dp) // Empty space for row headers

                        columnHeaderValues.forEach { category ->
                            nestedHeaderValuesByColumn[category]?.forEach { productName ->
                                val productCount =
                                    thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.size
                                        ?: 0
                                val nestedHeaderWidth = (productCount * 120).dp
                                if (productName != null) {
                                    TableCell(text = productName, width = nestedHeaderWidth)
                                }
                            }
                        }
                    }
                }

                // Render third header row if applicable
                if (columnHeaders.size > 2) {
                    Row(Modifier.fillMaxWidth()) {
                        TableCell(text = "", width = 120.dp) // Empty space for row headers

                        columnHeaderValues.forEach { category ->
                            nestedHeaderValuesByColumn[category]?.forEach { productName ->
                                thirdHeaderValuesByNestedHeader[Pair(
                                    category,
                                    productName
                                )]?.forEach { quality ->
                                    if (quality != null) {
                                        TableCell(text = quality, width = 120.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Render data rows based on number of column headers
                when (columnHeaders.size) {
                    1 -> generateRows(
                        filteredData,
                        rowHeaders,
                        columnHeaderValues,
                        nestedHeaderValuesByColumn,
                        thirdHeaderValuesByNestedHeader,
                        valueField,
                        columnHeaders[0],
                        "",
                        ""
                    )
                    2 -> generateRows(
                        filteredData,
                        rowHeaders,
                        columnHeaderValues,
                        nestedHeaderValuesByColumn,
                        thirdHeaderValuesByNestedHeader,
                        valueField,
                        columnHeaders[0],
                        columnHeaders[1],
                        ""
                    )
                    3 -> generateRows(
                        filteredData,
                        rowHeaders,
                        columnHeaderValues,
                        nestedHeaderValuesByColumn,
                        thirdHeaderValuesByNestedHeader,
                        valueField,
                        columnHeaders[0],
                        columnHeaders[1],
                        columnHeaders[2]
                    )
                }
            }
        }
    }
}


// Retrieves the value of a property from an object using reflection
inline fun <reified T : Any> getPropertyValue(obj: T, propertyName: String): Any? {
    return try {
        val property = T::class.memberProperties.find { it.name == propertyName }
        property?.getter?.call(obj)
    } catch (e: Exception) {
        null
    }
}
