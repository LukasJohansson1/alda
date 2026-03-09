package com.example;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;

// Write an implementation of the TreeSet class, with associated iterators, using a
// binary search tree. Add to each node a link to the next smallest and next largest
// node. To make your code simpler, add a header and tail node which are not part of
// the binary search tree, but help make the linked list part of the code simpler.

// Vi har ett binärtsöktrad med sökning och insättning/borttagning i O (log n) med en dubbellänkad lista som gör iteration i sorterad ordning enkelt.
// Intresanta metoder att gå igenom: add, remove, Iteratorerna, removeBST.

// Sorterad mängd baserad på en binärsökträd.
// Varje nod har prev/next-pekare som bildar en sorterad dubbellänkad lista.
// Två sentinel-noder (header, tail) ligger utanför trädet och fungerar som fasta gränser för listan,
// så att varje verklig nod alltid har ett icke-null prev och next.
// header - minsta - ... - större - tail


public class TreeSet<E extends Comparable<E>> implements Iterable<E> {

    private class Node {
        E data;
        Node left, right;   // barn i binärsökträdet
        Node prev, next;    // länkarna i den sorterade listan

        Node(E data) { this.data = data; }
    }

    private Node root;
    private final Node header = new Node(null);   // sentinel
    private final Node tail = new Node(null);   // sentinel
    private int size;
    private int modCount; // används för att upptäcka ändringar under iteration

    public TreeSet() {
        header.next = tail;
        tail.prev = header;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public E first() { // returnerar det minsta elementet, eller null om mängden är tom
        return (header.next == tail) ? null : header.next.data;
    }

    public E last() { // returnerar det största elementet, eller null om mängden är tom
        return (tail.prev == header) ? null : tail.prev.data;
    }

    // Vanlig BST-sökning
    public boolean contains(Object o) {
        if (o == null) throw new NullPointerException();
        @SuppressWarnings("unchecked") E key = (E) o; // Vi caster o till E för att kunna använda compareTo eftersom det kräver värde av typ E

        Node cur = root;
        while (cur != null) {
            int cmp = key.compareTo(cur.data); // jämför söknyckeln med nodens data
            if (cmp == 0) return true; // hittade elementet
            cur = (cmp < 0) ? cur.left : cur.right; // gå vänster eller höger beroende på jämförelsen
        }
        return false;
    }

    // Nya noder sätts alltid in som blad, och det gör listunderhållet enkelt:
    //   VÄNSTER barn -> nästa mindre värde  -> sätt in direkt INNAN föräldren i listan
    //   HÖGER barn   -> nästa större värde -> sätt in direkt EFTER föräldren i listan
    public boolean add(E element) {
        if (element == null) throw new NullPointerException();

        if (root == null) {
            root = new Node(element);
            linkAfter(header, root); // sätt in som första nod i listan, direkt efter header
            size++;
            modCount++;
            return true;
        }

        Node cur = root; // starta sökningen från roten
        while (true) { // loopa tills vi hittar rätt plats att sätta in det nya elementet
            int cmp = element.compareTo(cur.data); // jämför det nya elementet med nodens data
            if (cmp == 0) return false; // duplikat

            if (cmp < 0) { // om cmp är negativt, det nya elementet är mindre än nodens data, gå vänster
                if (cur.left == null) {
                    cur.left = new Node(element);
                    linkAfter(cur.prev, cur.left);  // länkar in den nya noden i listan, direkt före cur
                    size++;
                    modCount++;
                    return true;
                }
                cur = cur.left;
            } else {
                if (cur.right == null) {
                    cur.right = new Node(element);
                    linkAfter(cur, cur.right);      // efter cur
                    size++;
                    modCount++;
                    return true;
                }
                cur = cur.right; // om cmp är positivt, det nya elementet är större än nodens data, gå höger
            }
        }
    }

    // Söker upp noden och dess förälder, sedan delegerar till removeBST
    public boolean remove(Object o) {
        if (o == null) throw new NullPointerException();
        @SuppressWarnings("unchecked") E key = (E) o; // Vi caster o till E för att kunna använda compareTo eftersom det kräver värde av typ E

        Node parent = null, cur = root; // starta sökningen från roten
        while (cur != null) { // loopa tills vi hittar noden att ta bort eller når slutet av trädet
            int cmp = key.compareTo(cur.data); // jämför söknyckeln med nodens data
            if (cmp == 0) break;
            parent = cur;
            cur = (cmp < 0) ? cur.left : cur.right; // gå vänster eller höger beroende på jämförelsen
        }
        if (cur == null) return false; // elementet fanns inte i trädet

        root = removeBST(root, parent, cur); // ta bort noden från trädet och uppdatera root om det behövs
        size--;
        modCount++;
        return true;
    }

    /**
     * Tar bort {@code target} från binärsökträdet och från den länkade listan.
     *
     * <h3>Fall 1 – noll eller ett barn</h3>
     * Target tas bort från listan genom att hoppa över den (prev och next
     * pekar på varandra), och ersätts i trädet med dess enda barn (eller null).
     *
     * <h3>Fall 2 – två barn</h3>
     * Vi kan inte bara plocka bort target ur trädet, det hade brutit BST-ordningen.
     * Strategin är att {@code target} behåller sin plats i trädet men får ett
     * nytt värde, hämtat från dess <em> in-order-uppföljare</em> (den minsta noden
     * i höger delträd). Uppföljare-noden tas sedan bort istället.
     *
     * <p>Två viktiga egenskaper gör detta enkelt:
     * 
     * <ul>
     *      <p>Uppföljaren är alltid {@code target.next} i listan.
     *      In-order-uppföljaren är per definition nästa större värde, och det
     *      är precis vad {@code target.next} pekar på. Alltså räcker det att
     *      hoppa över uppföljaren i listan med två pekartilldelningar.
     *      Uppföljaren har som mest ett höger barn.
     *      Om den hade haft ett vänster barn hade det värdet ju vara mindre
     *      och hade valts som uppföljare istället. Alltså blir borttagningen
     *      av uppföljaren från trädet ett fall1 problem.
     * </ul>
     *
     * @param root    roten på trädet (kan ändras om target är roten)
     * @param parent  föräldern till target, eller {@code null} om target är roten
     * @param target  noden som ska tas bort
     * @return        ny rot (ändrad bara om target var den gamla roten och hade 0/1 barn)
     */
    private Node removeBST(Node root, Node parent, Node target) {

        if (target.left == null || target.right == null) { // Fall 1: target har noll eller ett barn
            // Hoppa över target i listan
            target.prev.next = target.next;
            target.next.prev = target.prev;

            Node child = (target.left != null) ? target.left : target.right; // det enda barnet, eller null om target är blad
            return replaceInParent(root, parent, target, child); // ersätt target med child i trädet, och returnera ny root
        }

        // Hitta uppföljaren. en steg höger, sedan hela vägen vänster
        Node succParent = target;
        Node successor = target.right;
        while (successor.left != null) {
            succParent = successor;
            successor = successor.left;
        }

        // Kopiera värdet; target stannar på plats i trädet
        target.data = successor.data;

        // Hoppa över uppföljaren i listan (successor == target.next)
        target.next = successor.next;
        successor.next.prev = target;

        // Ta bort uppföljaren ur trädet (har som mest ett höger barn)
        if (succParent == target)
            succParent.right = successor.right;
        else
            succParent.left = successor.right;

        return root;
    } // In-order-uppföljaren är den minsta noden i höger delträd, vi går ett steg höger, sedan hela vägen vänster tills vi inte kan mer. 
    // Om uppföljaren hade haft ett vänster barn hade det barnet varit mindre, och då hade det valts som uppföljare istället. 
    // Alltså är det en motsägelse att uppföljaren har ett vänster barn.

    // Ersätter oldNode med newNode hos parent. Om parent är null var oldNode roten.
    private Node replaceInParent(Node root, Node parent, Node oldNode, Node newNode) {
        if (parent == null) return newNode;
        if (parent.left == oldNode) 
            parent.left = newNode;
        else                          
            parent.right = newNode;
        return root;
    }

    public void clear() {
        root = null;
        header.next = tail;
        tail.prev = header;
        size = 0;
        modCount++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Node n = header.next; n != tail; n = n.next) {
            if (n != header.next) sb.append(", ");
            sb.append(n.data);
        }
        return sb.append("]").toString();
    }

    @Override
    public Iterator<E> iterator() {
        return new AscendingIterator();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    // Itererar via next-pekarna, från header till tail
    private class AscendingIterator implements Iterator<E> {
        private Node next = header.next;
        private Node lastReturned = null;
        private final int expectedMod = modCount; // används för att upptäcka ändringar under iterationen

        @Override public boolean hasNext() { return next != tail; }

        @Override
        public E next() { // returnerar nästa element i sorterad ordning
            checkMod();
            if (!hasNext()) throw new NoSuchElementException();
            lastReturned = next;
            next = next.next;
            return lastReturned.data;
        }

        @Override
        public void remove() { // tar bort den senast returnerade noden
            checkMod();
            if (lastReturned == null) throw new IllegalStateException();
            TreeSet.this.remove(lastReturned.data);
            lastReturned = null;
        }

        private void checkMod() { // om TreeSet ändrats under iterationen så kastas ConcurrentModificationException
            if (modCount != expectedMod) throw new ConcurrentModificationException();
        }
    }

    // Itererar via prev-pekarna, från tail till header
    private class DescendingIterator implements Iterator<E> {
        private Node next = tail.prev;
        private Node lastReturned = null;
        private final int expectedMod = modCount;

        @Override public boolean hasNext() { return next != header; }

        @Override
        public E next() { // returnerar nästa element i fallande ordning
            checkMod();
            if (!hasNext()) throw new NoSuchElementException();
            lastReturned = next;
            next = next.prev;
            return lastReturned.data;
        }

        @Override
        public void remove() { // tar bort den senast returnerade noden
            checkMod();
            if (lastReturned == null) throw new IllegalStateException();
            TreeSet.this.remove(lastReturned.data);
            lastReturned = null;
        }

        private void checkMod() { // om TreeSet ändrats under iterationen så kastas ConcurrentModificationException
            if (modCount != expectedMod) throw new ConcurrentModificationException();
        }
    }

    // Sätter in node i listan direkt efter 'after'
    private void linkAfter(Node after, Node node) {
        node.prev = after;
        node.next = after.next;
        after.next.prev = node;
        after.next = node;
    }

    // Skriver ut trädet sidledet: höger delträd överst, vänster underst
    public void printTree() {
        if (root == null) { System.out.println("(tomt träd)"); return; }
        printNode(root.right, "    ", true);
        System.out.println("── " + root.data + " (rot)");
        printNode(root.left, "    ", false);
    }

    private void printNode(Node node, String prefix, boolean isRight) {
        if (node == null) return;
        printNode(node.right, prefix + (isRight ? "    " : "│   "), true);
        System.out.println(prefix + (isRight ? "┌── " : "└── ") + node.data);
        printNode(node.left, prefix + (isRight ? "│   " : "    "), false);
    }

    public static void main(String[] args) {
        TreeSet<Integer> set = new TreeSet<>();

        int[] values = {50, 30, 70, 20, 40, 60, 80, 10, 25, 35, 45};
        for (int v : values) set.add(v);
        System.out.println("=== Träd efter insättning ===");
        set.printTree();

        System.out.println("\nadd(50) duplikat → " + set.add(50));
        System.out.println("Storlek      : " + set.size());
        System.out.println("Första       : " + set.first());
        System.out.println("Sista        : " + set.last());
        System.out.println("Innehåller 40: " + set.contains(40));
        System.out.println("Innehåller 99: " + set.contains(99));

        System.out.print("\nStigande : ");
        for (int v : set) System.out.print(v + " ");
        System.out.println();

        System.out.print("Fallande : ");
        Iterator<Integer> desc = set.descendingIterator();
        while (desc.hasNext()) System.out.print(desc.next() + " ");
        System.out.println();

        System.out.println("\n=== Ta bort 10 (blad) ===");
        set.remove(10);
        set.printTree();

        System.out.println("\n=== Ta bort 20 (ett barn) ===");
        set.remove(20);
        set.printTree();

        System.out.println("\n=== Ta bort 30 (två barn) ===");
        set.remove(30);
        set.printTree();

        System.out.println("\n=== Ta bort 50 (rot) ===");
        set.remove(50);
        set.printTree();

        System.out.println("\nTa bort 99 (saknas) → " + set.remove(99));

        set.clear();
        System.out.println("\n=== Efter rensa ===");
        set.printTree();
        System.out.println("Tomma : " + set.isEmpty());
    }
}