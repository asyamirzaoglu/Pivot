package com.example.pivot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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

// Generates rows for the pivot table
@Composable
fun generateRows(
    data: List<SalesData>,
    rowHeaders: List<String>,
    columnHeaderValues: List<String>,
    nestedHeaderValuesByColumn: Map<String, List<String?>>,
    thirdHeaderValuesByNestedHeader: Map<Pair<Any?, Any?>, List<String?>>,
    valuesField: String,
    columnHeaderName: String,
    nestedHeaderName: String,
    thirdHeaderName: String
) {
    val groupedData = groupByHeaders(data, rowHeaders)

    // Track colors for cell values
    val valueColors = mutableMapOf<Any?, Color>()

    // First pass to determine colors based on total sales amount
    groupedData.forEach { (firstHeaderValue, firstGroup) ->
        columnHeaderValues.forEach { category ->
            nestedHeaderValuesByColumn[category]?.forEach { productName ->
                thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.forEach { quality ->
                    val totalSalesAmount = firstGroup.filter {
                        getPropertyValue<SalesData>(it, columnHeaderName) == category &&
                                getPropertyValue<SalesData>(it, nestedHeaderName) == productName &&
                                getPropertyValue<SalesData>(it, thirdHeaderName) == quality
                    }.sumOf { getPropertyValue<SalesData>(it, valuesField) as Double }

                    val displayText = if (totalSalesAmount == 0.0) "-" else totalSalesAmount.toString()
                    valueColors[displayText] = Color(0xFFFFC0CB) // Set color for the value
                }
            }
        }
    }

    // Second pass to render rows with determined colors
    groupedData.forEach { (firstHeaderValue, firstGroup) ->
        val backgroundColor = Color(0xFFFFC0CB) // Color for the first row

        Row(Modifier.fillMaxWidth()) {
            TableCell(text = firstHeaderValue.toString(), width = 120.dp, backgroundColor = backgroundColor)

            columnHeaderValues.forEach { category ->
                nestedHeaderValuesByColumn[category]?.forEach { productName ->
                    if (columnHeaderName.isNotEmpty()) {
                        thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.forEach { quality ->
                            val totalSalesAmount = firstGroup.filter {
                                getPropertyValue<SalesData>(it, columnHeaderName) == category &&
                                        getPropertyValue<SalesData>(it, nestedHeaderName) == productName &&
                                        getPropertyValue<SalesData>(it, thirdHeaderName) == quality
                            }.sumOf { getPropertyValue<SalesData>(it, valuesField) as Double }

                            val displayText = if (totalSalesAmount == 0.0) "-" else totalSalesAmount.toString()
                            val cellColor = valueColors[displayText] ?: Color.Transparent
                            TableCell(text = displayText, width = 120.dp, backgroundColor = cellColor)
                        }
                    }
                }
            }
        }

        generateSubRows(
            firstGroup,
            rowHeaders.drop(1),
            columnHeaderValues,
            nestedHeaderValuesByColumn,
            thirdHeaderValuesByNestedHeader,
            valuesField,
            columnHeaderName,
            nestedHeaderName,
            thirdHeaderName
        )
    }
}

// Generates sub-rows for the pivot table
@Composable
fun generateSubRows(
    data: List<SalesData>,
    rowHeaders: List<String>,
    columnHeaderValues: List<String>,
    nestedHeaderValuesByColumn: Map<String, List<String?>>,
    thirdHeaderValuesByNestedHeader: Map<Pair<Any?, Any?>, List<String?>>,
    valuesField: String,
    columnHeaderName: String,
    nestedHeaderName: String,
    thirdHeaderName: String
) {
    if (rowHeaders.isEmpty()) return

    val groupedData = groupByHeaders(data, rowHeaders)

    // Define colors for cell values
    val valueColors = mutableMapOf<String, Color>()
    if (rowHeaders.size == 1) {
        valueColors["default"] = Color.Yellow
    }

    groupedData.forEach { (headerValue, group) ->
        val isFirstRow = rowHeaders.size == 1 // Determine if it's the first row in the remaining headers
        val backgroundColor = if (isFirstRow) Color.Yellow else Color.Transparent

        Row(Modifier.fillMaxWidth()) {
            TableCell(text = headerValue.toString(), width = 120.dp, backgroundColor = backgroundColor)

            // Calculate and display totals for each combination
            columnHeaderValues.forEach { category ->
                nestedHeaderValuesByColumn[category]?.forEach { productName ->
                    thirdHeaderValuesByNestedHeader[Pair(category, productName)]?.forEach { quality ->
                        val totalSalesAmount = group.filter {
                            getPropertyValue<SalesData>(it, columnHeaderName) == category &&
                                    getPropertyValue<SalesData>(it, nestedHeaderName) == productName &&
                                    getPropertyValue<SalesData>(it, thirdHeaderName) == quality
                        }.sumOf { getPropertyValue<SalesData>(it, valuesField) as Double }

                        val displayText = if (totalSalesAmount == 0.0) "-" else totalSalesAmount.toString()

                        // Determine cell color based on value
                        val cellColor = if (rowHeaders.size == 1) {
                            valueColors["default"] ?: Color.Transparent
                        } else {
                            Color.Transparent
                        }

                        TableCell(text = displayText, width = 120.dp, backgroundColor = cellColor)
                    }
                }
            }
        }

        // Recursively generate sub-rows
        generateSubRows(
            group,
            rowHeaders.drop(1),
            columnHeaderValues,
            nestedHeaderValuesByColumn,
            thirdHeaderValuesByNestedHeader,
            valuesField,
            columnHeaderName,
            nestedHeaderName,
            thirdHeaderName
        )
    }
}

// Groups data by specified headers
fun groupByHeaders(data: List<SalesData>, headers: List<String>): Map<Any?, List<SalesData>> {
    if (headers.isEmpty()) return emptyMap()

    val header = headers.first()
    return data.groupBy { getPropertyValue(it, header) }
}

// Renders a table cell with specified properties
@Composable
fun TableCell(text: String, width: Dp, backgroundColor: Color = Color.Transparent) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Black)
            .width(width)
            .padding(8.dp)
            .background(backgroundColor), // Apply background color
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
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
