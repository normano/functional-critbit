package io.prelink.critbit;

/**
 * An OO/Functional Java crit-bit tree, inspired by
 * djb (http://cr.yp.to/critbit.html),
 * Adam Langley (https://github.com/agl/critbit),
 * and Okasaki (http://www.eecs.usma.edu/webs/people/okasaki/pubs.html)
 */
public class CritBitTree<K,V> extends AbstractCritBitTree<K,V> {

    static class ShortLeftNode<K,V> extends AbstractInternal<K,V> {
        private final K leftKey;
        private final V leftVal;
        private final Node<K,V> right;
        public ShortLeftNode(int bit, K leftKey, V leftVal, Node<K,V> right) {
            super(bit);
            this.leftKey = leftKey;
            this.leftVal = leftVal;
            this.right = right;
        }
        public External<K,V> search(K key, Context<K,V> ctx) {
            if(ctx.chk.isSet(key, bit())) {
                return right.search(key, ctx);
            } else {
                return ctx.nf.mkLeaf(this.leftKey, this.leftVal);
            }
        }
        public Node<K,V> left(Context<K,V> ctx) { return ctx.nf.mkLeaf(leftKey, leftVal); }
        public Node<K,V> right(Context<K,V> ctx) { return right; }
        public Node<K,V> setLeft(int diffBit, K key, V val, Context<K,V> ctx) {
            Node<K,V> newLeft = mkShortBothChild(diffBit, key, val, leftKey, leftVal, ctx);
            return ctx.nf.mkTall(bit(), newLeft, right);
        }
        public Node<K,V> setRight(int diffBit, K key, V val, Context<K,V> ctx) {
            Node<K,V> newRight = right.insert(diffBit, key, val, ctx);
            return ctx.nf.mkShortLeft(bit(), leftKey, leftVal, newRight);
        }
    }
    static class ShortRightNode<K,V> extends AbstractInternal<K,V> {
        private final Node<K,V> left;
        private final K rightKey;
        private final V rightVal;
        public ShortRightNode(int bit, Node<K,V> left, K rightKey, V rightVal) {
            super(bit);
            this.left = left;
            this.rightKey = rightKey;
            this.rightVal = rightVal;
        }
        public External<K,V> search(K key, Context<K,V> ctx) {
            if(ctx.chk.isSet(key, bit())) {
                return ctx.nf.mkLeaf(this.rightKey, this.rightVal);
            } else {
                return left.search(key, ctx);
            }
        }
        public Node<K,V> left(Context<K,V> ctx) { return left; }
        public Node<K,V> right(Context<K,V> ctx) { return ctx.nf.mkLeaf(rightKey, rightVal); }
        public Node<K,V> setLeft(int diffBit, K key, V val, Context<K,V> ctx) {
            Node<K,V> newLeft = left.insert(diffBit, key, val, ctx);
            return ctx.nf.mkShortRight(bit(), newLeft, rightKey, rightVal);
        }
        public Node<K,V> setRight(int diffBit, K key, V val, Context<K,V> ctx) {
            Node<K,V> newRight = mkShortBothChild(diffBit, key, val, rightKey, rightVal, ctx);
            return ctx.nf.mkTall(bit(), left, newRight);
        }
    }
    static class TallNode<K,V> extends AbstractInternal<K,V> {
        private final Node<K,V> left;
        private final Node<K,V> right;
        public TallNode(int bit, Node<K,V> left, Node<K,V> right) {
            super(bit);
            this.left = left;
            this.right = right;
        }
        public External<K,V> search(K key, Context<K,V> ctx) {
            if(ctx.chk.isSet(key, bit())) {
                return right.search(key, ctx);
            } else {
                return left.search(key, ctx);
            }
        }
        public Node<K,V> left(Context<K,V> ctx) { return left; }
        public Node<K,V> right(Context<K,V> ctx) { return right; }
        public Node<K,V> setLeft(int diffBit, K key, V val, Context<K,V> ctx) {
            Node<K,V> newLeft = left.insert(diffBit, key, val, ctx);
            return ctx.nf.mkTall(bit(), newLeft, right);
        }
        public Node<K,V> setRight(int diffBit, K key, V val, Context<K,V> ctx) {
            Node<K,V> newRight = right.insert(diffBit, key, val, ctx);
            return ctx.nf.mkTall(bit(), left, newRight);
        }
    }

    static class ImmutableNodeFactory<K,V> implements NodeFactory<K,V> {
        public Internal<K,V> mkShortBoth(int diffBit, K lk, V lv, K rk, V rv) {
            return new ShortBothNode<K,V>(diffBit, lk, lv, rk, rv);
        }
        public Internal<K,V> mkShortRight(int diffBit, Node<K,V> left, K k, V v) {
            return new ShortRightNode<K,V>(diffBit, left, k, v);
        }
        public Internal<K,V> mkShortLeft(int diffBit, K k, V v, Node<K,V> right) {
            return new ShortLeftNode<K,V>(diffBit, k, v, right);
        }
        public Internal<K,V> mkTall(int diffBit, Node<K,V> left, Node<K,V> right) {
            return new TallNode<K,V>(diffBit, left, right);
        }
        public External<K,V> mkLeaf(K key, V val) {
            return new LeafNode<K,V>(key, val);
        }
    }

    private final Node<K,V> root;

    public CritBitTree(BitChecker<K> bitChecker) {
        this(null, new Context<K,V>(bitChecker, new ImmutableNodeFactory<K,V>()));
    }

    private CritBitTree(Node<K,V> root, Context<K,V> context) {
        super(context);
        this.root = root;
    }

    Node<K,V> root() { return root; }

    public CritBitTree<K,V> insert(K key, V val) {
        if(root() == null) {
            return new CritBitTree<K,V>(ctx().nf.mkLeaf(key, val), ctx());
        }
        External<K,V> ext = root().search(key, ctx());
        int i = ctx().chk.firstDiff(key, ext.key());
        return new CritBitTree<K,V>(root().insert(i, key, val, ctx()), ctx());
    }

}