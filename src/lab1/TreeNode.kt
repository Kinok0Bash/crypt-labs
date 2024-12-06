package lab1

data class TreeNode(
    var symbol: Char = '\u0000',
    var probability: Double,
    var left: TreeNode? = null,
    var right: TreeNode? = null
) : Comparable<TreeNode> {
    override fun compareTo(other: TreeNode): Int = probability.compareTo(other.probability)
}
